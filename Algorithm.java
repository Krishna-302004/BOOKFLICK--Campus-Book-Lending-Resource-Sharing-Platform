package com.bookshare;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Algorithm {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/bookdb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    private static final String INSERT_SQL =
            "INSERT INTO rating_values (id, R, N, GP) " +
                    "SELECT r.id, AVG(r.rating), (AVG(r.rating) / 5), " +
                    "((AVG(r.rating) / 5) * (AVG(r.rating) / 5) * 10) " +
                    "FROM ratings_BQ r " +
                    "GROUP BY r.id";

    public void insertRatingValues() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {

            int rowsInserted = stmt.executeUpdate();
            System.out.println(rowsInserted + " rows inserted into rating_values.");

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Algorithm inserter = new Algorithm();
        inserter.insertRatingValues();
    }
}
