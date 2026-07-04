package com.bookshare;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

import static java.util.logging.Logger.global;

public class BookSearch {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(9096), 0);
        server.createContext("/", new SearchHandler());
        server.createContext("/view", new ViewHandler());
        server.createContext("/rate", new RateHandler());
        server.setExecutor(null);
        //server.start();
        System.out.println("Server started at http://localhost:9096/");

        server.createContext("/requestBook", new RequestBookHandler());
        server.createContext("/bookDetails", new BookDetailsHandler());
// Other handlers...
        server.start();
    }

    public static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    public class UserContext {
        private static int sid = -1; // Default value indicating no user

        public static void setSid(int sid) {
            UserContext.sid = sid;
        }

        public static int getSid() {
            return sid;
        }
    }


    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String searchTerm = null;
            String filterDept = null;
            Integer filterSemester = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        String key = kv[0];
                        String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        switch (key) {
                            case "search" -> searchTerm = val;
                            case "department" -> filterDept = val;
                            case "semester" -> {
                                try {
                                    filterSemester = Integer.valueOf(val);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                }
            }

            List<String> departments = new ArrayList<>();
            List<Integer> semesters = new ArrayList<>();
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root")) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rd = stmt.executeQuery("SELECT DISTINCT department FROM books ORDER BY department")) {
                        while (rd.next()) {
                            departments.add(escape(rd.getString("department")));
                        }
                    }

                    try (Statement stmt2 = conn.createStatement();
                         ResultSet rs2 = stmt2.executeQuery("SELECT DISTINCT semester FROM books ORDER BY semester")) {
                        while (rs2.next()) {
                            semesters.add(rs2.getInt("semester"));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<String> names = new ArrayList<>();
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                StringBuilder sql = new StringBuilder("SELECT name FROM books");
                List<String> conditions = new ArrayList<>();
                if (searchTerm != null && !searchTerm.isEmpty()) conditions.add("name LIKE ?");
                if (filterDept != null && !filterDept.isEmpty()) conditions.add("department = ?");
                if (filterSemester != null) conditions.add("semester = ?");
                if (!conditions.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", conditions));

                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root");
                     PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    int idx = 1;
                    if (searchTerm != null && !searchTerm.isEmpty()) ps.setString(idx++, "%" + searchTerm + "%");
                    if (filterDept != null && !filterDept.isEmpty()) ps.setString(idx++, filterDept);
                    if (filterSemester != null) ps.setInt(idx, filterSemester);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            names.add(escape(rs.getString("name")));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang=\"en\"><head>")
                    .append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                    .append("<title>Book Search</title>")
                    .append("<link href=\"https://cdnjs.cloudflare.com/ajax/libs/tailwindcss/2.2.19/tailwind.min.css\" rel=\"stylesheet\">")
                    .append("</head><body class=\"bg-gray-100 text-gray-800 font-sans antialiased\">")
                    .append("<div class=\"container mx-auto p-6\">")
                    .append("<form method=\"get\" action=\"/\" class=\"mb-8 bg-white shadow-lg p-6 rounded-lg space-y-4\">")
                    .append("<h1 class=\"text-2xl font-semibold text-center text-blue-600\">Search for Books</h1>")
                    .append("<input name=\"search\" type=\"text\" placeholder=\"Search book name...\" class=\"border p-3 w-full rounded-lg shadow-sm focus:ring-2 focus:ring-blue-400\" ")
                    .append("value=\"").append(searchTerm != null ? escape(searchTerm) : "").append("\"/>")
                    .append("<div class=\"flex space-x-4\">")
                    .append("<select name=\"department\" class=\"border p-3 w-full rounded-lg shadow-sm focus:ring-2 focus:ring-blue-400\">")
                    .append("<option value=\"\">All Departments</option>");
            for (String dept : departments) {
                html.append("<option value='").append(dept).append("'")
                        .append(filterDept != null && filterDept.equals(dept) ? " selected" : "")
                        .append(">").append(dept).append("</option>");
            }
            html.append("</select>")
                    .append("<select name=\"semester\" class=\"border p-3 w-full rounded-lg shadow-sm focus:ring-2 focus:ring-blue-400\">")
                    .append("<option value=\"\">All Semesters</option>");
            for (Integer sem : semesters) {
                html.append("<option value='").append(sem).append("'")
                        .append(filterSemester != null && filterSemester.equals(sem) ? " selected" : "")
                        .append(">Semester ").append(sem).append("</option>");
            }
            html.append("</select>")
                    .append("</div>")
                    .append("<button type=\"submit\" class=\"bg-gradient-to-r from-blue-500 to-blue-700 text-white px-6 py-3 rounded-lg w-full transition duration-300 hover:from-blue-400 hover:to-blue-600\">Filter</button>")
                    .append("</form><div class=\"grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8\">");

            for (String nm : names) {
                html.append("<div class=\"bg-white p-6 rounded-lg shadow-lg transition-transform transform hover:scale-105 hover:shadow-xl\">")
                        .append("<p class=\"font-semibold text-lg text-gray-700 mb-4\">").append(nm).append("</p>")
                        .append("<form method=\"get\" action=\"/view\">")
                        .append("<input type=\"hidden\" name=\"name\" value=\"").append(nm).append("\"/>")
                        .append("<button type=\"submit\" class=\"bg-green-500 text-white px-6 py-2 rounded-lg transition duration-300 hover:bg-green-600\">View Details</button>")
                        .append("</form></div>");
            }

            html.append("</div></div></body></html>");

            byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class ViewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String bookName = null;

            // Parse query parameters
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        String key = kv[0];
                        String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        if ("name".equals(key)) {
                            bookName = val;
                        }
                    }
                }
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><title>View Book</title>")
                    .append("<link href=\"https://cdnjs.cloudflare.com/ajax/libs/tailwindcss/2.2.19/tailwind.min.css\" rel=\"stylesheet\">")
                    .append("</head><body class=\"p-6\">");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root")) {
                    String sql = "SELECT * FROM books WHERE name LIKE ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, "%" + bookName + "%");

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {  // Process only the first result
                                int bookId = rs.getInt("book_id");
                                int sid = rs.getInt("id"); // Assuming 'id' is the student ID in the 'books' table

                                html.append("<h1 class=\"text-2xl font-bold mb-4\">Book Details</h1>")
                                        .append("<div class=\"bg-gray-100 p-6 rounded space-y-2\">")
                                        .append("<p><strong>Name:</strong> ").append(escape(rs.getString("name"))).append("</p>")
                                        .append("<input type='hidden' name='sid' value='").append(sid).append("'/>")
                                        .append("<p><strong>Student Name:</strong> ").append(escape(rs.getString("std_name"))).append("</p>")
                                        .append("<p><strong>STdid:</strong> ").append(rs.getInt("id")).append("</p>")
                                        .append("<p><strong>Type:</strong> ").append(escape(rs.getString("type"))).append("</p>")
                                        .append("<p><strong>Subject:</strong> ").append(escape(rs.getString("subject"))).append("</p>")
                                        .append("<p><strong>Course Code:</strong> ").append(escape(rs.getString("course_code"))).append("</p>")
                                        .append("<p><strong>Department:</strong> ").append(escape(rs.getString("department"))).append("</p>")
                                        .append("<p><strong>Semester:</strong> ").append(rs.getInt("semester")).append("</p>")
                                        .append("<p><strong>Upload Year:</strong> ").append(rs.getInt("upload_year")).append("</p>")
                                        .append("<button class='bg-green-600 text-white px-4 py-2 rounded mb-4'>Request Book</button>") // This is the new button
                                        .append("</div>");

                                html.append("<div class='mt-6'><h2 class='text-xl font-semibold mb-2'>Ratings & Feedback</h2>");
                                try (PreparedStatement ps2 = conn.prepareStatement("SELECT id, rating, feedback, created_at FROM ratings_bq WHERE book_id = ?")) {
                                    ps2.setInt(1, bookId);
                                    try (ResultSet rs2 = ps2.executeQuery()) {
                                        int count = 0, sum = 0;
                                        List<String> feedbacks = new ArrayList<>();
                                        while (rs2.next()) {
                                            int r = rs2.getInt("rating");
                                            int idVal = rs2.getInt("id");
                                            String fb = escape(rs2.getString("feedback"));
                                            String createdAt = rs2.getString("created_at");
                                            sum += r;
                                            count++;
                                            feedbacks.add("<div class='mb-2'><p><strong>ID:</strong> " + idVal +
                                                    " | " + "⭐".repeat(r) + " - " + fb +
                                                    " <span class='text-gray-500 text-sm'>(" + createdAt + ")</span></p></div>");
                                        }
                                        if (count > 0) {
                                            double avg = (double) sum / count;
                                            html.append("<p><strong>Average Rating:</strong> ").append(String.format("%.1f", avg)).append(" / 5</p>");
                                        } else {
                                            html.append("<p>No ratings yet.</p>");
                                        }
                                        html.append("<div class='mt-2'>").append(String.join("", feedbacks)).append("</div>");
                                    }

                                }

                                // Leave a Rating form with user ID field
                                html.append("<div class='mt-6'><h2 class='text-xl font-semibold mb-2'>Leave a Rating</h2>")
                                        .append("<form method='post' action='/rate' class='space-y-2'>")
                                        .append("<input type='hidden' name='bookId' value='").append(bookId).append("'/>")
                                        .append("<input type='hidden' name='sid' value='").append(sid).append("'/>") // Hidden field for student ID
                                        .append("<label class='block'>User ID :</label>")
                                        .append("<input type='number' name='id' value='").append(sid).append("' readonly class='border p-2 w-full'/>") // Display sid for user, but not editable
                                        .append("<label class='block'>Star Rating (1-5):</label>")
                                        .append("<input type='number' name='rating' min='1' max='5' required class='border p-2 w-full'/>")
                                        .append("<label class='block'>Feedback:</label>")
                                        .append("<textarea name='feedback' class='border p-2 w-full' required></textarea>")
                                        .append("<button type='submit' class='bg-blue-600 text-white px-4 py-2 rounded'>Submit</button>")
                                        .append("</form></div>");
                            } else {
                                html.append("<p>No books found with the specified filters.</p>");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                html.append("</body></html>");
                byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class RequestBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Parse the form parameters
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> formData = parseFormData(body);

                int bookId = Integer.parseInt(formData.get("bookId"));
                int sid = Integer.parseInt(formData.get("sid"));

                // Insert into request table
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root")) {
                    String sql = "INSERT INTO request (book_id, req_id) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, bookId); // book_id in request table
                        ps.setInt(2, sid);    // req_id (student ID) in request table
                        ps.executeUpdate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    // In case of error, send error response
                    String errorResponse = "Error processing your request. Please try again.";
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorResponse.getBytes());
                    }
                    return;
                }

                // Redirect to the book details page with bookId as parameter
                String redirectURL = "/bookDetails?id=" + bookId;
                exchange.getResponseHeaders().set("Location", redirectURL);
                exchange.sendResponseHeaders(302, -1); // 302 Found for redirection
                exchange.getResponseBody().close();
            }
        }

        private Map<String, String> parseFormData(String body) {
            Map<String, String> formData = new HashMap<>();
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    formData.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                }
            }
            return formData;
        }
    }
    static class BookDetailsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Extract the book ID from the URL query parameters
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);
            int bookId = Integer.parseInt(params.get("id"));

            StringBuilder htmlResponse = new StringBuilder();
            htmlResponse.append("<!DOCTYPE html><html><head><title>Book Details</title></head><body>");

            // Get book details from database
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root")) {
                String sql = "SELECT * FROM books WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, bookId);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        // Display book details
                        htmlResponse.append("<h1>Book Details</h1>");
                        htmlResponse.append("<p><strong>ID:</strong> ").append(rs.getInt("id")).append("</p>");
                        htmlResponse.append("<p><strong>Title:</strong> ").append(rs.getString("title")).append("</p>");
                        htmlResponse.append("<p><strong>Author:</strong> ").append(rs.getString("author")).append("</p>");
                        // Add more book details as needed
                    } else {
                        htmlResponse.append("<h1>Book Not Found</h1>");
                        htmlResponse.append("<p>The requested book could not be found.</p>");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                htmlResponse.append("<h1>Error</h1>");
                htmlResponse.append("<p>An error occurred while retrieving book details.</p>");
            }

            htmlResponse.append("</body></html>");

            String response = htmlResponse.toString();
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        params.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                    }
                }
            }
            return params;
        }
    }
    static class RateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // Read the POST data
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
            StringBuilder buf = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }

            // Parse the form data
            Map<String, String> params = new HashMap<>();
            for (String param : buf.toString().split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }

            // Get rating form data
            String bookIdStr = params.get("bookId");
            String sidStr = params.get("sid"); // Student ID
            String ratingStr = params.get("rating");
            String feedback = params.get("feedback");

            if (bookIdStr != null && sidStr != null && ratingStr != null && feedback != null) {
                try {
                    int bookId = Integer.parseInt(bookIdStr);
                    int sid = Integer.parseInt(sidStr); // Convert student ID
                    int rating = Integer.parseInt(ratingStr);

                    // Insert the rating and feedback into the database
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root")) {
                        String insertQuery = "INSERT INTO ratings_BQ (book_id, id, rating, feedback, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
                        try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                            ps.setInt(1, bookId);  // Link the rating with the bookId
                            ps.setInt(2, sid);      // Student ID (from the form)
                            ps.setInt(3, rating);   // Rating value
                            ps.setString(4, feedback);  // User feedback
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // After rating, redirect to search page
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
        }

    }

}

