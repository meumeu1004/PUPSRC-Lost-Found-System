package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DATABASE_URL =
            System.getenv("DB_URL");

    private static final String DATABASE_USER =
            System.getenv("DB_USER");

    private static final String DATABASE_PASSWORD =
            System.getenv("DB_PASSWORD");
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
    }

    }
