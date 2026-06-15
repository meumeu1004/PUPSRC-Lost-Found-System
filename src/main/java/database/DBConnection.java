package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import io.github.cdimascio.dotenv.Dotenv;

public class DBConnection {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String DATABASE_URL =
            require(dotenv.get("DB_URL"), "DB_URL is missing in .env");

    private static final String DATABASE_USER =
            require(dotenv.get("DB_USER"), "DB_USER is missing in .env");

    private static final String DATABASE_PASSWORD =
            require(dotenv.get("DB_PASSWORD"), "DB_PASSWORD is missing in .env");

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
