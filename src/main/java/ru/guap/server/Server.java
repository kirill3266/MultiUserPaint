package ru.guap.server;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.awt.image.*;

public class Server {
    private ServerSocket serverSocket = null;
    private HashMap<String, BufferedImage> boards = null;
    private ArrayList<ClientThread> clients = null;
    private Object consoleSynch;

    class ClientThread extends Thread {
        // ПЕРЕМЕННЫЕ СЕТИ
        private Socket clientSocket = null;
        private BufferedReader readSocket = null;
        private BufferedWriter writeSocket = null;
        private String boardName = null;

        // ПЕРЕМЕННЫЕ ГРАФИКИ
        Color mainColor = null;
        Graphics2D graphics = null;

        public ClientThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                readSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writeSocket = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            } catch (IOException err) {
                synchronized (consoleSynch) {
                    System.out.println(err.getMessage());
                }
            }
        }

        public void run() {
            synchronized (consoleSynch) {
                System.out.println("Клиент подключился");
                synchronized (clients) {
                    System.out.println("Кол-во клиентов: " + clients.size() + "\n");
                }
            }
            try {
                try {
                    while (true) {
                        String message = readSocket.readLine();
                        String[] splitMessage = message.split(" ", 2);
                        if (splitMessage[0].equals("CREATE")) {
                            /*****************
                             * СОЗДАНИЕ ДОСКИ
                             *****************/
                            boolean isContains;
                            synchronized (boards) {
                                isContains = boards.containsKey(splitMessage[1]);
                            }
                            if (isContains) {
                                synchronized (this) {
                                    writeSocket.write("CREATE EXISTS\n");
                                    writeSocket.flush();
                                }
                            } else {
                                synchronized (this) {
                                    writeSocket.write("CREATE OK\n");
                                    writeSocket.flush();
                                }
                                String boardNameOld = boardName;

                                boardName = splitMessage[1];
                                synchronized (boards) {
                                    boards.put(boardName, new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB));
                                    graphics = boards.get(boardName).createGraphics();
                                }
                                synchronized (boards.get(boardName)) {
                                    graphics.setColor(Color.white);
                                    graphics.fillRect(0, 0, 800, 600);
                                }
                                synchronized (consoleSynch) {
                                    System.out.println("Доска \"" + boardName + "\" создана");
                                    synchronized (boards) {
                                        System.out.println("Кол-во досок: " + boards.size() + "\n");
                                    }
                                }

                                checkBoards(boardNameOld);
                            }
                        } else if (splitMessage[0].equals("CONNECT")) {
                            /**********************
                             * ПОДКЛЮЧЕНИЕ К ДОСКЕ
                             **********************/
                            boolean isContains;
                            synchronized (boards) {
                                isContains = boards.containsKey(splitMessage[1]);
                            }
                            if (isContains) {
                                synchronized (this) {
                                    writeSocket.write("CONNECT OK\n");
                                    writeSocket.flush();
                                }
                                String boardNameOld = boardName;

                                boardName = splitMessage[1];
                                synchronized (boards.get(boardName)) {
                                    graphics = boards.get(boardName).createGraphics();
                                }
                                int[] rgbArray = new int[480000];
                                synchronized (boards.get(boardName)) {
                                    boards.get(boardName).getRGB(0, 0, 800, 600, rgbArray, 0, 800);
                                }
                                synchronized (this) {
                                    for (int i = 0; i < rgbArray.length; i++) {
                                        writeSocket.write(rgbArray[i] + "\n");
                                        writeSocket.flush();
                                    }
                                }
                                checkBoards(boardNameOld);
                            } else {
                                synchronized (this) {
                                    writeSocket.write("CONNECT NOT FOUND\n");
                                    writeSocket.flush();
                                }
                            }
                        } else if (boardName != null) {
                            /*********************
                             * РИСОВАНИЕ НА ДОСКЕ
                             *********************/
                            splitMessage = message.split(" ", 4);
                            int color = Integer.parseInt(splitMessage[0]);
                            int coordX = Integer.parseInt(splitMessage[1]);
                            int coordY = Integer.parseInt(splitMessage[2]);
                            int size = Integer.parseInt(splitMessage[3]);
                            synchronized (boards.get(boardName)) {
                                graphics.setColor(new Color(color));
                                graphics.fillOval(coordX, coordY, size, size);
                            }

                            // ВСЕМ КТО ПОДКЛЮЧЕН
                            synchronized (clients) {
                                for (ClientThread iClient : clients) {
                                    if (iClient.boardName != null && iClient.boardName.equals(boardName)) {
                                        synchronized (iClient) {
                                            iClient.writeSocket.write(message + "\n");
                                            iClient.writeSocket.flush();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    clientSocket.close();
                    readSocket.close();
                    writeSocket.close();
                    synchronized (clients) {
                        clients.remove(this);
                        synchronized (consoleSynch) {
                            System.out.println("Клиент недоступен");
                            System.out.println("Кол-во клиентов: " + clients.size());
                        }
                    }
                    checkBoards(boardName);
                }
            } catch (Exception err) {
                synchronized (consoleSynch) {
                    System.out.println(err.toString() + "\n");
                }
            }
        }

    }

    public Server() {
        boards = new HashMap<String, BufferedImage>();
        clients = new ArrayList<ClientThread>();
        consoleSynch = new Object();
        try {
            serverSocket = new ServerSocket(0);
            System.out.println("PORT: " + serverSocket.getLocalPort());
            while (true) {
                ClientThread newClient = new ClientThread(serverSocket.accept());
                synchronized (clients) {
                    clients.add(newClient);
                    clients.get(clients.size() - 1).start();
                }
            }
        } catch (IOException err) {
            System.out.println(err.getMessage());
        }
    }

    void checkBoards(String boardName) {
        if (boardName == null) {
            return;
        }
        boolean boardUsed = false;
        for (ClientThread iClient : clients) {
            synchronized (iClient) {
                if (iClient.boardName != null && iClient.boardName.equals(boardName)) {
                    boardUsed = true;
                    break;
                }
            }
        }
        if (!boardUsed) {
            synchronized (boards) {
                boards.remove(boardName);
                synchronized (consoleSynch) {
                    System.out.println("Доска \"" + boardName + "\" не используется и была удалена");
                    System.out.println("Кол-во досок: " + boards.size() + "\n");
                }
            }
        }
    }
}