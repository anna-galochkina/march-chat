package ru.geekbrains.march.chat.server;

import java.sql.SQLException;

public interface AuthenticationProvider {
    void init();
    String getNicknameByLoginAndPassword(String login, String password) throws SQLException;
    boolean changeNickname(String oldNickname, String newNickname) throws SQLException;
    boolean isNicknameBusy(String nickname) throws SQLException;
    void shutdown();
}
