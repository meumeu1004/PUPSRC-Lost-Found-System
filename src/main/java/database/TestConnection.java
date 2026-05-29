package database;

import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {
//
            if (connection != null) {
                System.out.println("Connected to Supabase!");
            }

        } catch (Exception e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
        }
    }
}