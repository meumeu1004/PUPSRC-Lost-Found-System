package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DATABASE_URL      = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String DATABASE_USER     = "postgres.bjjfhgutepegvjlpmiqi";
    private static final String DATABASE_PASSWORD = "[Y9rWxZDH1SbaebgV)!";

    private static String require(String value, String errorMsg) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(errorMsg);
        }
        return value;
    }

    // Custom exception so controllers can catch connectivity failures
    // specifically without inspecting message strings
    public static class NoConnectionException extends RuntimeException {
        public NoConnectionException(Throwable cause) {
            super("Unable to reach the database. Please check your internet connection.", cause);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
        } catch (SQLException e) {
            throw new NoConnectionException(e);
        }
    }
}
