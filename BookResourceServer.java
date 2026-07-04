package com.bookshare;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;

public class BookResourceServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9095), 0);
        server.createContext("/books", new BookPageHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("BookResourceServer running at http://localhost:9095/books");
    }

    static class BookPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract query parameter (id) from the URL
            String query = exchange.getRequestURI().getQuery();
            String id = null;
            if (query != null && query.contains("id=")) {
                id = query.split("=")[1];  // Extract the value of "id"
            }

            // Fetch activity points (GP) from the database using the id
            String activityPoints = getActivityPoints(id);

            // HTML template with dynamic GP (activity points)
            String html = String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>BOOKFLICK - Share and Discover Study Resources</title>
                  <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                        font-family: Arial, sans-serif;
                    }

                    body {
                        background-color: #f4f4f4;
                        padding: 20px;
                    }

                    .container {
                        max-width: 900px;
                        margin: 0 auto;
                    }

                    h1 {
                        color: #333;
                        margin-bottom: 5px;
                        font-size: 28px;
                    }

                    .subtitle {
                        color: #555;
                        margin-bottom: 20px;
                        font-size: 14px;
                    }

                    .points-box {
                        background-color: #3a5573;
                        color: white;
                        padding: 20px;
                        border-radius: 8px;
                        text-align: center;
                        margin-bottom: 20px;
                    }

                    .points-count {
                        font-size: 36px;
                        font-weight: bold;
                        margin: 10px 0;
                    }

                    .points-text {
                        margin-bottom: 10px;
                    }

                    .card-container {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 20px;
                        justify-content: space-between;
                    }

                    .card {
                        background-color: white;
                        border-radius: 8px;
                        padding: 20px;
                        box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                        width: calc(33.33%% - 14px);
                        text-align: center;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        min-height: 240px;
                    }

                    .card img {
                        height: 50px;
                        margin-bottom: 15px;
                    }

                    .card h3 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 18px;
                    }

                    .card p {
                        color: #666;
                        margin-bottom: 15px;
                        font-size: 14px;
                        flex-grow: 1;
                    }

                    .btn {
                        background-color: #3a5573;
                        color: white;
                        padding: 10px 15px;
                        border-radius: 4px;
                        text-decoration: none;
                        font-size: 14px;
                        display: block;
                        width: 100%%;
                        text-align: center;
                        transition: background-color 0.3s;
                    }

                    .btn:hover {
                        background-color: #2c4056;
                    }

                    @media screen and (max-width: 768px) {
                        .card {
                            width: 100%%;
                            margin-bottom: 15px;
                        }
                    }
                  </style>
                </head>
                <body>
                <div class="container">
                  <h1>Welcome to BOOKFLICK</h1>
                  <p class="subtitle">Your one-stop platform for sharing and discovering study resources</p>

                  <div class="points-box">
                    <h2>Your Activity Points</h2>
                    <div class="points-count">%s</div>
                    <p class="points-text">Keep sharing resources to earn more points!</p>
                  </div>

                  <div class="card-container">
                    <div class="card">
                      <img src="https://cdn2.vectorstock.com/i/1000x1000/49/01/find-book-logo-icon-design-vector-22504901.jpg" alt="Books Icon">
                      <h3>Find Books</h3>
                      <p>Browse and find books from other students</p>
                      <a href="http://localhost:9096/" class="btn">Browse Books</a>
                    </div>

                    <div class="card">
                      <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRjXfDcRrfxSZKZ4T1JvQQbzTkv3TnyOXhPMw&s" alt="Resources Icon">
                      <h3>My Uploads</h3>
                      <p>View your shared resources and other study materials</p>
                      <a href="http://localhost:9094/books?id=%s" class="btn">Find My Uploads</a>
                    </div>

                    <div class="card">
                      <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzJLlpTEq-T8s37gwy4ctyWBaoG8Ra13aNA&s" alt="Upload Icon">
                      <h3>Upload and Available Resources</h3>
                      <p>Share your books or study materials</p>
                      <a href="http://localhost:9098/" class="btn">Upload or check Availabliliy </a>
                    </div>

                    <div class="card">
                      <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTm0Bu_SY4EqpYFD0mZsTqiMDR2SYGlXkrDdw&s" alt="Trophy Icon">
                      <h3>My Contributions</h3>
                      <p> Veiw rating based convertions</p>
                      <a href="http://localhost:9099?id=%s" class="btn">View Points</a>
                    </div>
                  </div>
                </div>
                </body>
                </html>
                """, activityPoints, id, id);

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }

        // Method to query the database and retrieve the activity points (GP) for a given user ID
        private String getActivityPoints(String id) {
            String activityPoints = "0";  // Default points

            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root")) {
                String query = "SELECT GP FROM rating_values WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, id);  // Set the student ID parameter
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            activityPoints = rs.getString("GP");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return activityPoints;
        }
    }
}