package ru.geekbrains.march.chat.server;

public interface AuthenticationProvider {
    String getNicknameByLoginAndPassword(String login, String password);
    boolean changeNickname(String oldNickname, String newNickname);
}
