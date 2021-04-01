package ru.geekbrains.march.chat.server;

import java.sql.*;
import java.util.List;

public class DbAuthenticationProvider implements AuthenticationProvider {
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement ps;

    private class User {
        private String login;
        private String password;
        private String nickname;

        public User(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    public DbAuthenticationProvider() {
        try {
            connect();
            createTable();
            fillTable();
        } catch (SQLException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    private static void createTable() throws SQLException {
        stmt.executeUpdate("CREATE TABLE if not exists users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "login VARCHAR, " +
                "password VARCHAR, " +
                "nickname VARCHAR " +
                ")");
    }

    private static void fillTable() throws SQLException {
        ps = connection.prepareStatement("SELECT * FROM users LIMIT 1;");
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                connection.setAutoCommit(false);
                stmt.addBatch(String.format("insert into users (login, password, nickname) values ('%s', '%s', '%s');", "John", "pass1", "John1"));
                stmt.addBatch(String.format("insert into users (login, password, nickname) values ('%s', '%s', '%s');", "Max", "pass2", "Max1"));
                stmt.addBatch(String.format("insert into users (login, password, nickname) values ('%s', '%s', '%s');", "Bob", "pass3", "Bob1"));
                stmt.executeBatch();
                connection.commit();
            }
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException {
        ps = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?;");
        ps.setString(1, login);
        ps.setString(2, password);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("nickname");
            }
        }
        return null;
    }

    @Override
    public boolean changeNickname(String oldNickname, String newNickname) throws SQLException {
        ps = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?;");
        ps.setString(1, newNickname);
        ps.setString(2, oldNickname);
        return ps.executeUpdate() > 0;
    }

    @Override
    public boolean isNicknameBusy(String nickname) throws SQLException {
        ps = connection.prepareStatement("SELECT id FROM users WHERE nickname = ?");
        ps.setString(1, nickname);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:march_chat.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Невозможно подключиться к БД");
        }
    }

    public static void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (ps != null) {
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
