package ru.guap;

import javax.swing.*;

import ru.guap.client.*;
import ru.guap.server.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Hello world!
 *
 */
public class App {

    public static boolean checkIPv4(final String ip) {
        boolean isIPv4;

        if(Objects.equals(ip, "localhost"))
            return true;

        try {
            final InetAddress inet = InetAddress.getByName(ip);
            isIPv4 = inet.getHostAddress().equals(ip)
                    && inet instanceof Inet4Address;
        } catch (final UnknownHostException e) {
            isIPv4 = false;
        }

        return isIPv4;
    }


    public static void main(String[] args) throws IOException {
        // первый аргумент при вызове определяет, вызываем мы сервер или клиент
        if (args.length != 0) {

            if (args[0].toUpperCase().equals("-S")) {

                System.out.println("SERVER");
                Server server = new Server(Integer.parseInt(args[1]));
            } else if (args[0].toUpperCase().equals("-C") && args.length == 3) {
                // 2-ой и 3-й - адрес и порт сервера, к которому будет подключаться клиент
                System.out.println("CLIENT");
                if(checkIPv4(args[1])) {
                    InetAddress serverIp = InetAddress.getByName(args[1]);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Client client = new Client(serverIp, Integer.parseInt(args[2]));
                        }
                    });
                }
            } else {
                System.out.println("[тип(-S/-S)] [адрес сервера(опционально)] [порт сервера(опционально)]");
            }
        } else {
            System.out.println("[тип(-S/-C)] [адрес сервера(опционально)] [порт сервера(опционально)]");
        }
    }
}