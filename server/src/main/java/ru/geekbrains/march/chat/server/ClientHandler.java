package ru.geekbrains.march.chat.server;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler implements Runnable {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        new Thread(() -> {
            try {
                authorize();
                communication();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void communication() throws IOException, SQLException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/")) {
                executeCommand(msg);
                continue;
            }
            server.broadcastMessage(username + ": " + msg);
            log(username + ": " + msg);
        }
    }

    private void authorize() throws IOException, SQLException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/login ")) {
                String[] tokens = msg.split("\\s+");
                if (tokens.length != 3) {
                    sendMessage("/login_failed Введите имя пользователя и пароль");
                    continue;
                }
                String userNickname = server.getAuthenticationProvider().getNicknameByLoginAndPassword(tokens[1], tokens[2]);
                if (userNickname == null) {
                    sendMessage("/login_failed Введен некорректный логин/пароль");
                    continue;
                }
                if (server.isUserOnline(userNickname)) {
                    sendMessage("/login_failed Учетная запись уже используется");
                    continue;
                }
                username = userNickname;
                sendMessage("/login_ok " + username);
                server.subscribe(this);
                break;
            }
        }
    }

    private void executeCommand(String cmd) throws SQLException {
        if (cmd.startsWith("/w ")) {
            String[] tokens = cmd.split("\\s+", 3);
            if (tokens.length != 3) {
                sendMessage("Server: Введена некорректная команда");
                return;
            }
            server.sendPrivateMessage(this, tokens[1], tokens[2]);
            return;
        }
        if (cmd.startsWith("/change_nick ")) {
            String[] tokens = cmd.split("\\s+");
            if (tokens.length != 2) {
                sendMessage("Server: Введена некорректная команда");
                return;
            }
            String newNickname = tokens[1];
            if (server.getAuthenticationProvider().isNicknameBusy(newNickname)) {
                sendMessage("Server: Такой никнейм уже занят");
                return;
            }
            if (!server.getAuthenticationProvider().changeNickname(username, newNickname)) {
                sendMessage("Server: Не удалось сменить никнейм");
                return;
            }
            username = newNickname;
            sendMessage("Server: Вы изменили никнейм на " + newNickname);
            server.broadcastClientsList();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            disconnect();
        }
    }

    public void log(String message) {
        try {
            if (!message.startsWith("/")) {
                File file = new File("log.txt");
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        throw new Exception("Server: Не удалось создать файл логов");
                    }
                }
                try (FileWriter writer = new FileWriter("log.txt", true)) {
                    writer.write(message);
                    writer.append('\n');
                    writer.flush();
                } catch (IOException ex) {
                    throw new Exception("Server: Не удалось записать данные в файл логов");
                }
            }
        } catch (Exception e) {
            sendMessage(e.getMessage());
        }

    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
