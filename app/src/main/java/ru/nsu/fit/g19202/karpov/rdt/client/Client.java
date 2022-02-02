package ru.nsu.fit.g19202.karpov.rdt.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import ru.nsu.fit.g19202.karpov.rdt.RDT;
import ru.nsu.fit.g19202.karpov.rdt.exceptions.ProcessNotRespondException;

public class Client {
    public static void main(String args[]) throws ClassNotFoundException, UnknownHostException, IOException, ProcessNotRespondException {
        RDT rdt = new RDT();
        rdt.init(InetAddress.getLocalHost(), 8888);
        rdt.send("Give me 20 strings".getBytes());
        byte[] result = rdt.receive();
        while (result != null) {
            System.out.println(new String(result));
            result = rdt.receive();
        }
    }
}
