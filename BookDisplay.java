//own book displayy

package com.bookshare;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BookDisplay {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9094), 0);
        server.createContext("/", new BookListHandler());
        server.setExecutor(null); // default executor
        System.out.println("Server started at http://localhost:9094/books?id=${studentId}");
        server.start();
    }

    static class BookListHandler implements HttpHandler {

        private static final String DB_URL = "jdbc:mysql://localhost:3306/bookdb";
        private static final String DB_USER = "root";
        private static final String DB_PASS = "root";
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery(); // Get query string like id=101
            int studentId = -1;

            if (query != null && query.startsWith("id=")) {
                try {
                    studentId = Integer.parseInt(query.substring(3));
                } catch (NumberFormatException e) {
                    studentId = -1;
                }
            }
            StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Student Books</title>
                  <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
                </head>
                <body class="bg-gray-100 p-6">
                <h1 class="text-2xl font-bold mb-4">Your Uploaded Books</h1>
                <div class="space-y-4">
                """);

            if (studentId != -1) {
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    String sql = "SELECT name FROM books WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, studentId);
                        ResultSet rs = stmt.executeQuery();

                        boolean hasBooks = false;

                        while (rs.next()) {
                            hasBooks = true;
                            String bookName = rs.getString("name");
                            String encodedBookName = bookName.replace(" ", "%20");

                            html.append("""
                                <div class="bg-white p-4 rounded shadow flex justify-between items-center">
                                    <span class="text-lg font-medium">""")
                                    .append(bookName)
                                    .append("""
                                    </span>
                                    <a href="/view?name=""")
                                    .append(encodedBookName)
                                    .append("""
                                    " class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">View</a>
                                </div>
                                """);
                        }

                        if (!hasBooks) {
                            html.append("<p class='text-red-500'>You haven't uploaded any books yet.</p>");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    html.append("<p style='color:red;'>Error loading books. Please try again later.</p>");
                }
            } else {
                html.append("<p style='color:red;'>Invalid Student ID.</p>");
            }

            html.append("""
                </div>
                </body>
                </html>
                """);
            byte[] response = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);


            }
        }
    }
}
