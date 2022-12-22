package ru.guap.client;

import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.image.*;
import java.awt.event.*;

public class Client {
    // ПЕРЕМЕННЫЕ СЕТИ
    boolean isConnected = false;
    InetAddress serverHost = null;
    int serverPort;
    Socket clientSocket;
    BufferedReader readSocket;
    BufferedWriter writeSocket;

    // ПЕРЕМЕННЫЕ ГРАФИКИ
    JFrame frame;
    JToolBar toolbar; // кнопки
    JPanel menu; // меню
    JLabel existLabel; // доска существует
    JLabel notFoundLabel; // доска не неайдена
    BoardPanel boardPanel; // отображение доски
    BufferedImage board = null; // доска
    Graphics2D graphics;
    Color mainColor;
    int size = 10; // размер кисти

    class BoardPanel extends JPanel implements Serializable {
        private static final long serialVersionUID = -109728024865681281L;

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(board, 0, 0, this);
        }
    }


    /*************************************
     * КЛАСС СЧИТЫВАНИЕ ДАННЫХ ОТ СЕРВЕРА
     *************************************/
    class NetDraw extends Thread {
        String message;
        String[] splitMessage;

        public NetDraw() {
            this.start();
        }

        public void run() {
            try {
                try {
                    while (true) {
                        message = readSocket.readLine();
                        splitMessage = message.split(" ", 2);
                        if (splitMessage[0].equals("CREATE")) {
                            /*****************
                             * СОЗДАНИЕ ДОСКИ
                             *****************/
                            if (splitMessage[1].equals("OK")) {
                                board = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
                                graphics = board.createGraphics();
                                graphics.setColor(Color.white);
                                graphics.fillRect(0, 0, 800, 600);
                                isConnected = true;
                                frame.remove(menu);
                                frame.add(boardPanel);
                                frame.repaint();
                            } else if (splitMessage[1].equals("EXISTS")) {
                                menu.add(existLabel);
                                frame.repaint();
                            }
                        } else if (splitMessage[0].equals("CONNECT")) {
                            /**********************
                             * ПОДКЛЮЧЕНИЕ К ДОСКЕ
                             **********************/
                            if (splitMessage[1].equals("OK")) {
                                int[] rgbArray = new int[480000];
                                for (int i = 0; i < rgbArray.length; i++) {
                                    message = readSocket.readLine();
                                    rgbArray[i] = Integer.parseInt(message);
                                }
                                board = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
                                board.setRGB(0, 0, 800, 600, rgbArray, 0, 800);
                                graphics = board.createGraphics();
                                isConnected = true;
                                frame.remove(menu);
                                frame.add(boardPanel);
                                frame.repaint();
                            } else if (splitMessage[1].equals("NOT FOUND")) {
                                menu.add(notFoundLabel);
                                frame.repaint();
                            }
                        } else {
                            /*********************
                             * РИСОВАНИЕ НА ДОСКЕ
                             *********************/
                            splitMessage = message.split(" ", 4);
                            int color = Integer.parseInt(splitMessage[0]);
                            int coordX = Integer.parseInt(splitMessage[1]);
                            int coordY = Integer.parseInt(splitMessage[2]);
                            int size = Integer.parseInt(splitMessage[3]);

                            graphics.setColor(new Color(color));
                            graphics.fillOval(coordX, coordY, size, size);
                            boardPanel.repaint();
                        }
                    }
                } catch (IOException err) {
                    System.out.println(err);
                    readSocket.close();
                    writeSocket.close();
                }
            } catch (IOException err) {
                System.out.println(err);
            }
        }
    }

    public Client(InetAddress serverHost, int serverPort) {
        // СЕТЬ
        try {
            try {
                this.serverHost = serverHost;
                this.serverPort = serverPort;
                clientSocket = new Socket(serverHost, serverPort);
                readSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writeSocket = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                new NetDraw();
            } catch (IOException err) {
                System.out.println(err);
                readSocket.close();
                writeSocket.close();
            }
        } catch (IOException err) {
            System.out.println(err);
        }

        // ГРАФИКА
        frame = new JFrame("MultiPaint");
        frame.setSize(840, 600); // размер окна
        frame.setResizable(false); // нельзя менять размер окна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // закрытие программы
        frame.setLayout(null);
        frame.setVisible(true);

        // ******************
        // ПАНЕЛЬ РИСОВАНИЯ
        // ******************
        boardPanel = new BoardPanel();
        boardPanel.setBounds(40, 0, 800, 600);
        boardPanel.setOpaque(true);
        mainColor = Color.white; // Color нынешний цвет

        // *************
        // ПАНЕЛЬ МЕНЮ
        // *************
        menu = new JPanel();
        menu.setBounds(40, 0, 800, 600);
        menu.setBackground(mainColor);
        menu.setLayout(null);
        frame.add(menu);

        // ДОСКА С ТАКИМ ИМЕНЕМ УЖЕ СУЩЕСТВУЕТ
        ImageIcon exist = new ImageIcon(this.getClass().getClassLoader().getResource("exist.png"));
        existLabel = new JLabel(exist);
        existLabel.setBounds(20, 85, 200, 30);

        // ДОСКА С ТАКИМ ИМЕНЕМ НЕ НАЙДЕНА
        ImageIcon notFound = new ImageIcon(this.getClass().getClassLoader().getResource("notFound.png"));
        notFoundLabel = new JLabel(notFound);
        notFoundLabel.setBounds(20, 85, 200, 30);

        // ПРИГЛАШЕНИЕ
        ImageIcon invite = new ImageIcon(this.getClass().getClassLoader().getResource("invite.png"));
        JLabel inviteLabel = new JLabel(invite);
        inviteLabel.setBounds(20, 5, 200, 30);
        menu.add(inviteLabel);

        // ТЕКСТОВОЕ ПОЛЕ
        JTextField textField = new JTextField();
        textField.setBounds(20, 45, 200, 30);
        textField.setText("default");
        menu.add(textField);

        // СОЗДАТЬ ДОСКУ
        JButton createBoard = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("createBoard.png")));
        createBoard.setBounds(225, 40, 210, 40); // размещение
        createBoard.setBorderPainted(false); // не рисовать рамку
        createBoard.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        createBoard.setOpaque(false); // прозрачность
        createBoard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String nameBoard = textField.getText();
                // НЕТ ИМЕНИ ДОСКИ
                if (nameBoard.equals("")) {
                    frame.repaint();
                    return;
                }

                // УДАЛЕНИЕ ПРЕДУПРЕЖДЕНИЙ
                if (menu.isAncestorOf(existLabel)) {
                    menu.remove(existLabel);
                    frame.repaint();
                }
                if (menu.isAncestorOf(notFoundLabel)) {
                    menu.remove(notFoundLabel);
                    frame.repaint();
                }

                try {
                    try {
                        writeSocket.write("CREATE " + nameBoard + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err);
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err);
                }
            }
        });
        menu.add(createBoard);

        // ПОДКЛЮЧИТЬСЯ К ДОСКЕ
        JButton joinBoard = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("joinBoard.png")));
        joinBoard.setBounds(435, 40, 210, 40); // размещение
        joinBoard.setBorderPainted(false); // не рисовать рамку
        joinBoard.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        joinBoard.setOpaque(false); // прозрачность
        joinBoard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String nameBoard = textField.getText();
                // НЕТ ИМЕНИ ДОСКИ
                if (nameBoard.equals("")) {
                    System.out.println();
                    frame.repaint();
                    return;
                }

                // УДАЛЕНИЕ ПРЕДУПРЕЖДЕНИЙ
                if (menu.isAncestorOf(existLabel)) {
                    menu.remove(existLabel);
                    frame.repaint();
                }
                if (menu.isAncestorOf(notFoundLabel)) {
                    menu.remove(notFoundLabel);
                    frame.repaint();
                }

                // ПОДКЛЮЧЕНИЕ
                try {
                    try {
                        writeSocket.write("CONNECT " + nameBoard + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err);
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err);
                }
            }
        });
        menu.add(joinBoard);

        // ************************
        // ПАНЕЛЬ С ИНСТРУМЕНТАМИ
        // ************************
        JToolBar toolbar = new JToolBar("Toolbar", JToolBar.VERTICAL);
        toolbar.setBounds(0, 0, 40, 600); // размещение
        toolbar.setLayout(null); // элементы размещаем сами
        toolbar.setFloatable(false); // нельзя перетаскивать
        toolbar.setBorderPainted(false); // без рамок
        toolbar.setBackground(Color.gray); // устанавливаем цвет панели
        frame.add(toolbar);

        // МЕНЮ
        JButton menuButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("menu.png")));
        menuButton.setBounds(0, 0, 40, 40); // размещение
        menuButton.setBorderPainted(false); // не рисовать рамку
        menuButton.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        menuButton.setOpaque(false); // прозрачность
        menuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (isConnected) {
                    if (frame.isAncestorOf(menu)) {
                        frame.remove(menu);
                        frame.add(boardPanel);
                        frame.repaint();
                    } else {
                        frame.remove(boardPanel);
                        frame.add(menu);
                        frame.repaint();
                    }
                }
            }
        });
        toolbar.add(menuButton);

        // Смена размера
        JButton sizeButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size10.png")));
        sizeButton.setBounds(0, 40, 40, 40);
        sizeButton.setBorderPainted(false);
        sizeButton.setBackground(Color.lightGray);
        sizeButton.setOpaque(false);
        sizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                switch (size) {
                    case 10:
                        size = 20;
                        sizeButton.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("size20.png")));
                        break;
                    case 20:
                        size = 40;
                        sizeButton.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("size40.png")));
                        break;
                    case 40:
                        size = 80;
                        sizeButton.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("size80.png")));
                        break;
                    case 80:
                        size = 10;
                        sizeButton.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("size10.png")));
                        break;
                }
            }
        });
        toolbar.add(sizeButton);

        // ВЫБОР ЦВЕТА
        JButton chooser = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Палитра.png")));
        chooser.setBounds(0, 80, 40, 40);
        chooser.setBorderPainted(false);
        chooser.setBackground(Color.lightGray);
        chooser.setOpaque(false);
        chooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = JColorChooser.showDialog(null, "Choose a color", Color.WHITE);
            }
        });
        toolbar.add(chooser);


        // ЗАЛИВКА
        JButton filling = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Заливка.png")));
        filling.setBounds(0, 120, 40, 40);
        filling.setBorderPainted(false);
        filling.setBackground(Color.lightGray);
        filling.setOpaque(false);
        filling.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                graphics.setColor(mainColor);
                graphics.fillRect(0, 0, 800, 600);
                boardPanel.repaint();
            }
        });
        toolbar.add(filling);

        // Смена темы
        JButton theme = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("Тема-белая.png")));
        theme.setBounds(0, 160, 40, 40);
        theme.setBorderPainted(false);
        theme.setBackground(Color.lightGray);
        theme.setOpaque(false);
        theme.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (menu.getBackground() == Color.white) {
                    menu.setBackground(Color.black);
                    toolbar.setBackground(Color.magenta);
                    theme.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("Тема-тёмная.png")));
                    frame.setBackground(Color.black);
                } else {
                    menu.setBackground(Color.white);
                    toolbar.setBackground(Color.gray);
                    theme.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("Тема-белая.png")));
                    frame.setBackground(Color.white);
                }
            }
        });
        toolbar.add(theme);

        // ***********
        // СЛУШАТЕЛИ
        // ***********
        boardPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                try {
                    try {
                        String message = mainColor.getRGB() + " " + (e.getX() - size / 2) + " " + (e.getY() - size / 2)
                                + " " + size;
                        writeSocket.write(message + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err);
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err);
                }

            }
        });

        boardPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    try {
                        String message = mainColor.getRGB() + " " + (e.getX() - size / 2) + " " + (e.getY() - size / 2)
                                + " " + size;
                        writeSocket.write(message + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err);
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err);
                }
            }
        });
    }
}