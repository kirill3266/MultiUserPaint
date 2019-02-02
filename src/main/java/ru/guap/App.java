package ru.guap;

import javax.swing.*;

import ru.guap.client.*;
import ru.guap.server.*;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        // первый аргумент при вызове определяет, вызываем мы сервер или клиент
        if (args.length != 0) {
            if (args[0].toUpperCase().equals("-S")) {
                System.out.println("SERVER");
                Server server = new Server();
            } else if (args[0].toUpperCase().equals("-C") && args.length == 3) {
                // 2-ой и 3-й - адрес и порт сервера, к которому будет подключаться клиент
                System.out.println("CLIENT");
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Client client = new Client(args[1], Integer.parseInt(args[2]));
                    }
                });
            } else {
                System.out.println("[тип(-S/-S)] [адрес сервера(опционально)] [порт сервера(опционально)]");
            }
        } else {
            System.out.println("[тип(-S/-C)] [адрес сервера(опционально)] [порт сервера(опционально)]");
        }
    }
}