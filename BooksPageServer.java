//veiw
package com.bookshare;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BooksPageServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9094), 0); // New port
        server.createContext("/books", new BooksHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("BooksPageServer started at http://localhost:9094/books");
    }

    // Handler for the /books endpoint
    static class BooksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Book> books = fetchBooksFromDatabase();

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            html.append("<title>BOOKFLICK - Books</title>");
            html.append("<style>");
            // Include basic styling inline
            html.append("body{font-family:sans-serif;background:#f8fafc;margin:0;padding:20px;}h2{color:#6366f1;}");
            html.append(".book-card{background:white;padding:16px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:20px;border:1px solid #e5e7eb;}");
            html.append(".book-title{font-size:20px;color:#1f2937;font-weight:bold;}");
            html.append(".department{font-size:13px;color:#6b7280;margin-top:4px;}");
            html.append(".meta{font-size:13px;color:#6b7280;margin-top:4px;}");
            html.append(".uploaded{font-size:12px;color:#9ca3af;margin-top:10px;}");
            html.append("</style></head><body>");
            html.append("<h2>Books Available</h2>");

            for (Book book : books) {
                html.append("<div class='book-card'>");
                html.append("<div class='book-title'>" + book.name + "</div>");
                html.append("<div class='department'>" + book.department + "</div>");
                html.append("<div class='meta'>Subject: " + book.subject + " | Course Code: " + book.courseCode + "</div>");
                html.append("<div class='meta'>Type: " + book.type + "</div>");
                html.append("<div class='uploaded'>Uploaded by: " + book.stdName + "</div>");
                html.append("</div>");
            }

            html.append("</body></html>");

            byte[] response = html.toString().getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }

        private List<Book> fetchBooksFromDatabase() {
            List<Book> books = new ArrayList<>();
            try {
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/bookdb", "root", "root");

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM books");

                while (rs.next()) {
                    Book b = new Book(
                            rs.getString("name"),
                            rs.getString("std_name"),
                            rs.getString("type"),
                            rs.getString("subject"),
                            rs.getString("course_code"),
                            rs.getString("department")
                    );
                    books.add(b);
                }

                rs.close();
                stmt.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return books;
        }
    }

    // Updated Book class to match new schema
    static class Book {
        String name;
        String stdName;
        String type;
        String subject;
        String courseCode;
        String department;

        Book(String name, String stdName, String type, String subject, String courseCode, String department) {
            this.name = name;
            this.stdName = stdName;
            this.type = type;
            this.subject = subject;
            this.courseCode = courseCode;
            this.department = department;
        }
    }
}

