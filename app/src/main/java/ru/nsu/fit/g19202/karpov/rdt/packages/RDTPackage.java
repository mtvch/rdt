package ru.nsu.fit.g19202.karpov.rdt.packages;

import java.io.Serializable;
import java.net.InetAddress;

public class RDTPackage implements Serializable {
    private int seq;
    private int nck;
    private byte[] data = null;
    private boolean isAck = false;
    private boolean isSyn = false;
    private boolean isFin = false;
    private int sourcePort;
    private InetAddress sourceAddr;

    private static final long serialVersionUID = 123L; 

    public RDTPackage(final byte[] data, int nck, int seq) {
        this.seq = seq;
        this.nck = nck;
        this.data = data.clone();
    }

    public RDTPackage(int seq, int nck) {
        this.seq = seq;
        this.nck = nck;
    }

    public void setSource(InetAddress addr, int port) {
        this.sourceAddr = addr;
        this.sourcePort = port;
    }

    public void enableSyn() {
        isSyn = true;
    }

    public void enableAck() {
        isAck = true;
    }

    public void enableFin() {
        isFin = true;
    }

    public int getSeq() {
        return seq;
    }

    public int getNck() {
        return nck;
    }

    public boolean isAck() {
        return isAck;
    }

    public boolean isSyn() {
        return isSyn;
    }

    public boolean isFin() {
        return isFin;
    }

    public byte[] getData() {
        return data;
    }

    public InetAddress getSourceAddress() {
        return sourceAddr;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public String toString() {
        return  "SEQ: " + seq + System.lineSeparator() + "NCK: " + nck + System.lineSeparator() + "isAck: " + isAck + System.lineSeparator() + "isSyn: " + isSyn + System.lineSeparator() + "isFin: " + isFin + System.lineSeparator() + "DATA: " + data + System.lineSeparator();
    }
}