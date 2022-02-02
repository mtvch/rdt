package ru.nsu.fit.g19202.karpov.rdt.server;

import java.io.IOException;
import java.net.InetAddress;

import ru.nsu.fit.g19202.karpov.rdt.RDT;
import ru.nsu.fit.g19202.karpov.rdt.exceptions.ProcessNotRespondException;

public class Server {
    private static final int port = 8888;

    public static void main(String[] args) throws ClassNotFoundException, IOException, ProcessNotRespondException {
        RDT rdt = new RDT(InetAddress.getLocalHost(), port);
        rdt.accept();

        switch (new String(rdt.receive())) {
            case "Give me 20 strings":
                send20Strings(rdt);
                break;
            default:
                throw new IllegalStateException();
        }

        rdt.close();
    }

    private static void send20Strings(RDT rdt) throws ClassNotFoundException, IOException, ProcessNotRespondException {
        byte[][] messages = new byte[20][];
        for (int i = 0; i < 20; i++) {
            messages[i] = ("String " + i).getBytes();
        }
        rdt.send(messages);
    }
}