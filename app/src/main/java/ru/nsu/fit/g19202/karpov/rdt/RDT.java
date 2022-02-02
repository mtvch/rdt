package ru.nsu.fit.g19202.karpov.rdt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

import ru.nsu.fit.g19202.karpov.rdt.exceptions.*;
import ru.nsu.fit.g19202.karpov.rdt.packages.*;

public class RDT {
    private DatagramSocket socket;
    private InetAddress otherAddress;
    private int otherPort;
    private int nck = 0; // number of accepted packages (last seq of accpeted package)
    private int seq = 0; // number of sent packages
    private int dupCounter = 0;

    private static final int DUP_LIMIT = 5;
    private static final int BUFF_SIZE = 1024;
    private static final int TIMEOUT = 200;
    
    /**
     * Creates RDT, attached to specific address and port
     * @param myAddress
     * @param myPort
     * @throws SocketException
     */
    public RDT(final InetAddress myAddress, int myPort) throws SocketException {
        this.socket = new DatagramSocket(myPort, myAddress);
    }

    /**
     * creates RDT, attached to any available port on localhost
     * @throws SocketException
     */
    public RDT() throws SocketException {
        this.socket = new DatagramSocket();
    }

    /**
     * Initiates connection with server
     * @param serverAddress
     * @param serverPort
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ProcessNotRespondException
     */
    public void init(final InetAddress serverAddress, int serverPort) throws IOException, ClassNotFoundException, ProcessNotRespondException {
        this.socket.setSoTimeout(TIMEOUT);
        sendSyn(serverAddress, serverPort);
        verifySyn(serverAddress, serverPort);
        seq++;
        sendAck(serverAddress, serverPort);

        this.otherAddress = serverAddress;
        this.otherPort = serverPort;
    }

    /**
     * Waits until client connects
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ProcessNotRespondException
     */
    public void accept() throws IOException, ClassNotFoundException, ProcessNotRespondException {
        this.socket.setSoTimeout(0);
        RDTPackage pkg = waitSyn();
        sendSynAck(pkg.getSourceAddress(), pkg.getSourcePort());
        this.socket.setSoTimeout(TIMEOUT);
        this.otherPort = pkg.getSourcePort();
        this.otherAddress = pkg.getSourceAddress();
        verifySynAck(pkg.getSourceAddress(), pkg.getSourcePort());
        seq++;
    }

    /**
     * Closes the connection
     * @throws IOException
     * @throws ProcessNotRespondException
     * @throws ClassNotFoundException
     */
    public void close() throws IOException, ClassNotFoundException, ProcessNotRespondException {
        sendFin();
        verifyFin();
        seq++;
        sendAck(otherAddress, otherPort);

        this.socket.setSoTimeout(0);
    }

    /**
     * Reliably sends one message
     * @param message
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ProcessNotRespondException
     */
    public void send(final byte[] message) throws IOException, ClassNotFoundException, ProcessNotRespondException {
        if (seq == 0) {
            // TODO:
            // replace with custom exception
            throw new IllegalStateException("Connection must be established first");
        }

        RDTPackage sendPkg = new RDTPackage(message, nck, seq + 1);
        sendPackage(sendPkg, otherAddress, otherPort);

        verifyMessage(message);
        seq++;
    }
    
    /**
     * Reliably sends messages.length messages in pipeline
     * @param messages
     * @throws IOException
     * @throws ProcessNotRespondException
     * @throws ClassNotFoundException
     */
    public void send(final byte[][] messages) throws IOException, ClassNotFoundException, ProcessNotRespondException {
        if (seq == 0) {
            // TODO:
            // replace with custom exception
            throw new IllegalStateException("Connection must be established first");
        }

        for (int i = 0; i < messages.length; i++) {
            RDTPackage sendPkg = new RDTPackage(messages[i], nck, seq + i + 1);
            sendPackage(sendPkg, otherAddress, otherPort);
        }

        verifyPipeline(messages, 0);
        seq += messages.length;
    }


    /**
     * Recives messages during connection
     * @return message data or null if connection is closed
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ProcessNotRespondException
     */
    public byte[] receive() throws IOException, ClassNotFoundException, ProcessNotRespondException {
        if (seq == 0) {
            // TODO:
            // replace with custom exception
            throw new IllegalStateException("Connection hasn't been established yet");
        }

        RDTPackage pkg = getPackage();

        if (pkg.isAck()) {
            byte[] data = receive();
            return data;
        }

        if (pkg.getSeq() != nck + 1) {
            emptySocket();
            sendAck(otherAddress, otherPort);
            byte[] data = receive();
            return data;
        }

        nck++;
        if (pkg.isFin()) {
            sendFinAck();
            verifyClosure();
            seq++;
        } else {
            sendAck(otherAddress, otherPort);            
        }
        
        return pkg.getData();
    }

    // PRIVATE

    private void sendSyn(InetAddress addr, int port) throws IOException {
        RDTPackage pkg = new RDTPackage(seq + 1, nck);
        pkg.enableSyn();
        sendPackage(pkg, addr, port);
    }

    private void sendAck(InetAddress addr, int port) throws IOException {
        RDTPackage pkg = new RDTPackage(seq + 1, nck);
        pkg.enableAck();
        sendPackage(pkg, addr, port);
    }

    private void sendSynAck(InetAddress addr, int port) throws IOException {
        RDTPackage pkg = new RDTPackage(seq + 1, nck);
        pkg.enableSyn();
        pkg.enableAck();
        sendPackage(pkg, addr, port);
    }

    private void sendFinAck() throws IOException {
        RDTPackage pkg = new RDTPackage(seq + 1, nck);
        pkg.enableFin();
        pkg.enableAck();
        sendPackage(pkg, otherAddress, otherPort);
    }

    private void sendFin() throws IOException {
        RDTPackage pkg = new RDTPackage(seq + 1, nck);
        pkg.enableFin();
        sendPackage(pkg, otherAddress, otherPort);
    }

    private void verifySyn(InetAddress addr, int port) throws ClassNotFoundException, IOException, ProcessNotRespondException {
        try {
            RDTPackage pkg = receivePackage(addr, port);
            dupCounter = 0;

            //System.out.println(pkg);
            if (pkg.isSyn() && pkg.isAck() && pkg.getNck() == seq + 1) {
                nck++;
            } else {
                // TODO:
                // replace with custom exception
                throw new IllegalStateException("Doesn't follow protocol");
            }
        } catch (SocketTimeoutException e) {
            dupCounter++;
            if (dupCounter > DUP_LIMIT) {
                throw new ProcessNotRespondException();
            }
            sendSyn(addr, port);
            verifySyn(addr, port);
        }
    }

    private void verifyFin() throws ClassNotFoundException, IOException, ProcessNotRespondException {
        try {
            RDTPackage pkg = receivePackage(otherAddress, otherPort);
            dupCounter = 0;

            if (pkg.isFin() && pkg.isAck() && pkg.getNck() == seq + 1) {
                nck++;
            } else {
                // TODO:
                // replace with custom exception
                throw new IllegalStateException("Doesn't follow protocol");
            }
        } catch (SocketTimeoutException e) {
            dupCounter++;
            if (dupCounter > DUP_LIMIT) {
                throw new ProcessNotRespondException();
            }
            sendFin();
            verifyFin();
        }
    }

    private RDTPackage waitSyn() throws ClassNotFoundException, IOException {
        RDTPackage pkg = receivePackage();
        if (pkg.isSyn() && pkg.getSeq() == nck + 1) {
            nck++;
            return pkg;
        } else {
            // TODO:
            // replace with custom exception
            throw new IllegalStateException("Doesn't follow protocol");
        }
    }

    private RDTPackage receivePackage(InetAddress addr, int port) throws IOException, ClassNotFoundException {
        byte[] data = new byte[BUFF_SIZE];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        socket.receive(packet);

        randomLoss();
        
        if (!packet.getAddress().equals(addr) || packet.getPort() != port) {
            // TODO:
            // Replace with custom exception
            throw new IllegalStateException("Received message not from the server");
        }

        RDTPackage pkg;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bais)) {
            pkg = (RDTPackage)ois.readObject();
        }

        return pkg;
    }

    private RDTPackage receivePackage() throws IOException, ClassNotFoundException {
        byte[] data = new byte[BUFF_SIZE];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        socket.receive(packet);

        randomLoss();

        RDTPackage pkg;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bais)) {
            pkg = (RDTPackage)ois.readObject();
        }

        pkg.setSource(packet.getAddress(), packet.getPort());

        return pkg;
    }

    private void sendPackage(RDTPackage pkg, InetAddress addr, int port) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(pkg);
            oos.flush();
            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
            socket.send(packet);
        }
    }

    private void send(final byte[][]messages, int offset) throws IOException, ProcessNotRespondException, ClassNotFoundException {
        for (int i = offset; i < messages.length; i++) {
            RDTPackage sendPkg = new RDTPackage(messages[i], nck, seq + i + 1);
            sendPackage(sendPkg, otherAddress, otherPort);
        }

        verifyPipeline(messages, offset);
    }

    private void verifyMessage(byte[] message) throws ClassNotFoundException, IOException, ProcessNotRespondException {
        try {
            RDTPackage recvPkg = receivePackage(otherAddress, otherPort);
            //System.out.println(recvPkg);
            if (recvPkg.getNck() < seq + 1 && (recvPkg.isSyn() || recvPkg.getData() != null)) {
                sendAck(otherAddress, otherPort);
                verifyMessage(message);
            }
            dupCounter = 0;
        } catch (SocketTimeoutException e) {
            dupCounter++;
            if (dupCounter <= DUP_LIMIT) {
                send(message);
            } else {
                throw new ProcessNotRespondException();
            }
        }
    }

    private void verifyPipeline(byte[][] messages, int offset) throws ClassNotFoundException, IOException, ProcessNotRespondException  {
        int seqCounter = seq + offset;
        try {
            while (seqCounter < seq + messages.length) {
                RDTPackage recvPkg = receivePackage(otherAddress, otherPort);
                //System.out.println(recvPkg);
                //System.out.println("MY SEQ: " + seq);
                //System.out.println("SEQ COUNTER: " + seqCounter);
                if (recvPkg.getNck() < seq) {
                    sendAck(otherAddress, otherPort);
                    continue;
                }

                dupCounter = 0;
    
                if (recvPkg.getNck() > seqCounter) {
                    seqCounter = recvPkg.getNck();
                } else {
                    send(messages, seqCounter - seq);
                }
            }
        } catch (SocketTimeoutException e) {
            dupCounter++;
            if (dupCounter <= DUP_LIMIT) {
                send(messages, seqCounter - seq);
            } else {
                throw new ProcessNotRespondException();
            }
        }
    }

    private void verifySynAck(InetAddress addr, int port) throws ClassNotFoundException, IOException, ProcessNotRespondException {
        try {
            RDTPackage pkg = receivePackage(addr, port);
            dupCounter = 0;
            if (pkg.isSyn()) {
                sendSynAck(addr, port);
                verifySynAck(addr, port);
            } else if (pkg.getSeq() > nck + 1) {
                return;
            }
        } catch (SocketTimeoutException e) {
            dupCounter++;
            if (dupCounter > DUP_LIMIT) {
                throw new ProcessNotRespondException();
            }
            sendSynAck(addr, port);
            verifySynAck(addr, port);
        }
    }

    private void verifyClosure() throws IOException, ClassNotFoundException, ProcessNotRespondException {
        try {
            RDTPackage recvPkg = receivePackage(otherAddress, otherPort);
            dupCounter = 0;
            if (recvPkg.isFin()) {
                sendFinAck();
                verifyClosure();
            } else if (!recvPkg.isAck()) {
                throw new IllegalStateException("Does not follow protocol");
            }
        } catch (SocketTimeoutException e) {
            dupCounter++;
            if (dupCounter > DUP_LIMIT) {
                return;
            }
            sendFinAck();
            verifyClosure();
        }
    }

    private RDTPackage getPackage() throws ProcessNotRespondException, ClassNotFoundException, IOException {
        try {
            RDTPackage pkg = receivePackage();
            //System.out.println(pkg);
            dupCounter = 0;
            return pkg;
        } catch (SocketTimeoutException e) {
            dupCounter++;
            if (dupCounter <= DUP_LIMIT) {
                RDTPackage pkg = getPackage();
                return pkg;
            }
            throw new ProcessNotRespondException();
        }
    }

    private void randomLoss() throws SocketTimeoutException, SocketException {
        Random rand = new Random();
        int n = rand.nextInt(10);
        if (n > 7) {
            if (socket.getSoTimeout() > 0) {
                throw new SocketTimeoutException();
            }
        }
    }

    public void emptySocket() throws IOException {
        socket.setSoTimeout(5);
        byte[] data = new byte[BUFF_SIZE];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            while (true) {
                socket.receive(packet);
            }
        } catch (SocketTimeoutException e) {
            socket.setSoTimeout(TIMEOUT);
            return;
        }
    }
}