package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DATABASE_URL      = System.getenv("DB_URL");
    private static final String DATABASE_USER     = System.getenv("DB_USER");
    private static final String DATABASE_PASSWORD = System.getenv("DB_PASSWORD");

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
