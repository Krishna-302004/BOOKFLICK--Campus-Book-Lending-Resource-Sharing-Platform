package com.bookshare;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BookServer {
    public static void main(String[] args) throws IOException {
        // Create uploads directory if it doesn't exist
        File uploadsDir = new File("uploads");
        if (!uploadsDir.exists()) {
            uploadsDir.mkdir();
        }

        // Initialize database if needed
        initializeDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(9098), 0);
        server.createContext("/", new BookListHandler()); // Home page shows book list
        server.createContext("/upload", new BookUploadFormHandler()); // Separate page for upload form
       // server.createContext("/process-upload", new ViewPdfHandler.BookUploadProcessor()); // Process the upload
        server.createContext("/view", new ViewPdfHandler()); // For viewing PDFs
        server.createContext("/css", new CssHandler()); // For serving CSS
        server.setExecutor(null);
        server.start();
        System.out.println("StudyShare Server started at http://localhost:9098/");
    }

    // Initialize database tables if they don't exist
    private static void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bookdb", "root", "root")) {

                // Check if books table exists, if not create it
                DatabaseMetaData meta = conn.getMetaData();
                ResultSet tables = meta.getTables(null, null, "books", null);
                if (!tables.next()) {
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate(
                            "CREATE TABLE books (" +
                                    "book_id INT PRIMARY KEY, " +
                                    "id INT, " +
                                    "name VARCHAR(255), " +
                                    "std_name VARCHAR(255), " +
                                    "type VARCHAR(100), " +
                                    "subject VARCHAR(255), " +
                                    "course_code VARCHAR(50), " +
                                    "department VARCHAR(255), " +
                                    "semester INT, " +
                                    "upload_year VARCHAR(10), " +
                                    "pdf_path VARCHAR(255)" +
                                    ")"
                    );
                    System.out.println("Created books table in database");
                }
            }
        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // CSS Handler to serve stylesheet
    static class CssHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String css = """
                        :root {
                            --primary: #3a5573;
                            --primary-light: #4a6683;
                            --primary-dark: #2a4563;
                            --accent: #f7941e;
                            --text: #333333;
                            --text-light: #666666;
                            --background: #f5f7fa;
                            --white: #ffffff;
                            --border: #e0e0e0;
                            --shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                            --shadow-hover: 0 10px 15px rgba(0, 0, 0, 0.15);
                            --radius: 8px;
                            --radius-sm: 4px;
                        }
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        }
                        body {
                            background-color: var(--background);
                            color: var(--text);
                            line-height: 1.6;
                        }
                        .container {
                            max-width: 1200px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        header {
                            background-color: var(--white);
                            box-shadow: var(--shadow);
                            padding: 20px 0;
                            position: sticky;
                            top: 0;
                            z-index: 100;
                        }
                        .header-content {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                        }
                        .logo {
                            display: flex;
                            align-items: center;
                            gap: 10px;
                            color: var(--primary);
                            text-decoration: none;
                        }
                        .logo h1 {
                            font-size: 24px;
                            font-weight: 700;
                        }
                        .logo span {
                            color: var(--accent);
                        }
                        nav {
                            display: flex;
                            gap: 20px;
                        }
                        nav a {
                            text-decoration: none;
                            color: var(--primary);
                            font-weight: 500;
                            padding: 5px 10px;
                            border-radius: var(--radius-sm);
                            transition: all 0.3s ease;
                        }
                        nav a:hover {
                            background-color: var(--primary-light);
                            color: var(--white);
                        }
                        .main-content {
                            margin-top: 30px;
                        }
                        .upload-form {
                            background-color: var(--white);
                            border-radius: var(--radius);
                            box-shadow: var(--shadow);
                            padding: 25px;
                            max-width: 800px;
                            margin: 0 auto;
                        }
                        .form-title {
                            margin-bottom: 20px;
                            color: var(--primary);
                            font-size: 22px;
                            font-weight: 600;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                        }
                        .form-group {
                            margin-bottom: 15px;
                        }
                        label {
                            display: block;
                            margin-bottom: 5px;
                            font-weight: 500;
                            color: var(--text);
                        }
                        input[type="text"],
                        input[type="number"],
                        input[type="file"],
                        select {
                            width: 100%;
                            padding: 10px 15px;
                            border: 1px solid var(--border);
                            border-radius: var(--radius-sm);
                            font-size: 14px;
                            transition: all 0.3s ease;
                        }
                        input[type="text"]:focus,
                        input[type="number"]:focus,
                        input[type="file"]:focus,
                        select:focus {
                            outline: none;
                            border-color: var(--primary);
                            box-shadow: 0 0 0 2px rgba(58, 85, 115, 0.2);
                        }
                        button {
                            background-color: var(--primary);
                            color: var(--white);
                            border: none;
                            border-radius: var(--radius-sm);
                            padding: 12px 20px;
                            font-size: 16px;
                            font-weight: 500;
                            cursor: pointer;
                            transition: all 0.3s ease;
                            width: 100%;
                        }
                        button:hover {
                            background-color: var(--primary-dark);
                        }
                        .book-list {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
                            gap: 20px;
                        }
                        .book-card {
                            background-color: var(--white);
                            border-radius: var(--radius);
                            box-shadow: var(--shadow);
                            padding: 20px;
                            transition: all 0.3s ease;
                        }
                        .book-card:hover {
                            box-shadow: var(--shadow-hover);
                            transform: translateY(-5px);
                        }
                        .book-card h3 {
                            color: var(--primary);
                            margin-bottom: 10px;
                        }
                        .book-card p {
                            margin-bottom: 5px;
                            color: var(--text-light);
                        }
                        .book-card strong {
                            color: var(--text);
                        }
                        .book-card .view-btn {
                            display: inline-block;
                            margin-top: 15px;
                            background-color: var(--primary);
                            color: var(--white);
                            padding: 8px 15px;
                            text-decoration: none;
                            border-radius: var(--radius-sm);
                            transition: all 0.3s ease;
                        }
                        .book-card .view-btn:hover {
                            background-color: var(--primary-dark);
                        }
                        footer {
                            margin-top: 50px;
                            background-color: var(--primary);
                            color: var(--white);
                            padding: 20px 0;
                            text-align: center;
                            font-size: 14px;
                        }
                        .page-title {
                            text-align: center;
                            color: var(--primary);
                            margin-bottom: 30px;
                        }
                        .add-book-btn {
                            display: inline-block;
                            background-color: var(--accent);
                            color: var(--white);
                            padding: 12px 25px;
                            text-decoration: none;
                            border-radius: var(--radius-sm);
                            margin-bottom: 30px;
                            font-weight: 500;
                            transition: all 0.3s ease;
                        }
                        .add-book-btn:hover {
                            background-color: #e68200;
                        }
                        .center {
                            text-align: center;
                        }
                    """;

            exchange.getResponseHeaders().set("Content-Type", "text/css");
            exchange.sendResponseHeaders(200, css.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(css.getBytes());
            }
        }
    }

    // Home page to list all books
    static class BookListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder bookListHtml = new StringBuilder();
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/bookdb", "root", "root");
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM books")) {

                    while (rs.next()) {
                        int bookId = rs.getInt("book_id");
                        String bookName = rs.getString("name");

                        bookListHtml.append("<div class=\"book-card\">");
                        bookListHtml.append("<h3>").append(bookName).append("</h3>");
                        bookListHtml.append("<p><strong>Book ID:</strong> ").append(bookId).append("</p>");
                        bookListHtml.append("<p><strong>Student ID:</strong> ").append(rs.getInt("id")).append("</p>");
                        bookListHtml.append("<p><strong>Student:</strong> ").append(rs.getString("std_name")).append("</p>");
                        bookListHtml.append("<p><strong>Type:</strong> ").append(rs.getString("type")).append("</p>");
                        bookListHtml.append("<p><strong>Subject:</strong> ").append(rs.getString("subject")).append("</p>");
                        bookListHtml.append("<p><strong>Course Code:</strong> ").append(rs.getString("course_code")).append("</p>");
                        bookListHtml.append("<p><strong>Department:</strong> ").append(rs.getString("department")).append("</p>");
                        bookListHtml.append("<p><strong>Semester:</strong> ").append(rs.getInt("semester")).append("</p>");
                        bookListHtml.append("<p><strong>Year:</strong> ").append(rs.getString("upload_year")).append("</p>");

                        // Add View PDF button
                        bookListHtml.append("<a href='/view?id=").append(bookId)
                                .append("' target='_blank' class=\"view-btn\">View PDF</a>");
                        bookListHtml.append("</div>");
                    }
                }
            } catch (Exception e) {
                bookListHtml.append("<p>Error loading books: ").append(e.getMessage()).append("</p>");
                e.printStackTrace();
            }

            String homePage = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                      <meta charset="UTF-8">
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <title></title>
                      <link rel="stylesheet" href="/css">
                    </head>
                    <body>
                      <header>
                        <div class="container">
                          <div class="header-content">
                            <a href="/" class="logo">
                              <h1>BookFlick</h1>
                            </a>
                            <nav>
                    
                            </nav>
                          </div>
                        </div>
                      </header>
                    
                    
                    
                    
                      <div class="container">
                        <div class="main-content">
                          <h1 class="page-title">Available Books</h1>
                          <div class="center">
                            <a href="/upload" class="add-book-btn">+ Add New Book</a>
                          </div>
                          <div class="book-list">
                            <!-- Book list goes here -->
                    """ + bookListHtml.toString() + """
                          </div>
                        </div>
                      </div>
                    
                      <footer>
                        <div class="container">
                          <p>&copy; BookFlick - Book Sharing Platform. All rights reserved.</p>
                        </div>
                      </footer>
                    </body>
                    </html>
                    """;

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, homePage.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(homePage.getBytes());
            }
        }
    }

    // Separate page for upload form
    static class BookUploadFormHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uploadPage = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                      <meta charset="UTF-8">
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <title>Upload Book </title>
                      <link rel="stylesheet" href="/css">
                    </head>
                    <body>
                      <header>
                        <div class="container">
                          <div class="header-content">
                            <a href="/" class="logo">
                    
                            </a>
                            <nav>
                              <a href="/">Home</a>
                    
                            </nav>
                          </div>
                        </div>
                      </header>
                    
                    
                    
                      <div class="container">
                        <div class="main-content">
                          <h1 class="page-title">Upload a Book</h1>
                          <div class="upload-form">
                            <form action="/process-upload" method="post" enctype="multipart/form-data">
                              <div class="form-group">
                                <label for="book_name">Book Name</label>
                                <input type="text" id="book_name" name="book_name" required placeholder="Enter book name">
                              </div>
                    
                              <div class="form-group">
                                <label for="book_id">Book ID</label>
                                <input type="number" id="book_id" name="book_id" required placeholder="Enter book id">
                              </div>
                    
                              <div class="form-group">
                                <label for="std_name">Student Name</label>
                                <input type="text" id="std_name" name="std_name" required placeholder="Your name">
                              </div>
                    
                              <div class="form-group">
                                <label for="std_id">Student ID</label>
                                <input type="number" id="std_id" name="std_id" required placeholder="Enter student id">
                              </div>
                    
                              <div class="form-group">
                                <label for="type">Type</label>
                                <input type="text" id="type" name="type" required placeholder="Textbook, Notes, etc.">
                              </div>
                    
                              <div class="form-group">
                                <label for="subject">Subject</label>
                                <input type="text" id="subject" name="subject" required placeholder="Enter subject">
                              </div>
                    
                              <div class="form-group">
                                <label for="course_code">Course Code</label>
                                <input type="text" id="course_code" name="course_code" required placeholder="e.g. CS101">
                              </div>
                    
                              <div class="form-group">
                                <label for="department">Department</label>
                                <input type="text" id="department" name="department" required placeholder="Enter department">
                              </div>
                    
                              <div class="form-group">
                                <label for="semester">Semester</label>
                                <select id="semester" name="semester" required>
                                  <option value="">Select semester</option>
                                  <option value="1">1</option>
                                  <option value="2">2</option>
                                  <option value="3">3</option>
                                  <option value="4">4</option>
                                  <option value="5">5</option>
                                  <option value="6">6</option>
                                  <option value="7">7</option>
                                  <option value="8">8</option>
                                </select>
                              </div>
                    
                              <div class="form-group">
                                <label for="upload_year">Upload Year</label>
                                <input type="number" id="upload_year" name="upload_year" required placeholder="Enter year">
                              </div>
                    
                              <div class="form-group">
                                <label for="pdf">Upload PDF</label>
                                <input type="file" id="pdf" name="pdf" accept="application/pdf" required>
                              </div>
                    
                              <button type="submit">Upload Book</button>
                            </form>
                          </div>
                        </div>
                      </div>
                    
                      <footer>
                        <div class="container">
                          <p>&copy; BookFlick - Book Sharing Platform. All rights reserved.</p>
                        </div>
                      </footer>
                    </body>
                    </html>
                    """;

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, uploadPage.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(uploadPage.getBytes());
            }
        }
    }

    // Handler for viewing PDF files
    static class ViewPdfHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String bookIdStr = query.split("=")[1];
            int bookId = Integer.parseInt(bookIdStr);

            String pdfPath = "";

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/bookdb", "root", "root");
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT pdf_path FROM books WHERE book_id = ?")) {

                    stmt.setInt(1, bookId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            pdfPath = rs.getString("pdf_path");
                        }
                    }
                }
            } catch (Exception e) {
                sendError(exchange, "Database error: " + e.getMessage());
                return;
            }

            if (pdfPath == null || pdfPath.isEmpty()) {
                sendError(exchange, "PDF file not found for book ID: " + bookId);
                return;
            }

            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                // If the full path doesn't work, try just the filename
                pdfFile = new File("uploads/book_" + bookId + ".pdf");
                if (!pdfFile.exists()) {
                    sendError(exchange, "PDF file not found on server");
                    return;
                }
            }

            // Set headers for PDF response
            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            exchange.getResponseHeaders().set("Content-Disposition", "inline; filename=\"book_" + bookId + ".pdf\"");
            exchange.sendResponseHeaders(200, pdfFile.length());

            // Send PDF file data
            try (OutputStream os = exchange.getResponseBody();
                 InputStream is = new FileInputStream(pdfFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }

        private void sendError(HttpExchange exchange, String errorMessage) throws IOException {
            String errorPage = "<html><body><h1>Error</h1><p>" + errorMessage + "</p><p><a href='/'>Return to home</a></p></body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(500, errorPage.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorPage.getBytes());
            }
        }
    }

    // For sending error messages
    private static void sendError(HttpExchange exchange, String errorMessage) throws IOException {
        String errorPage = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Error - BookFlick</title>
                  <link rel="stylesheet" href="/css">
                </head>
                <body>
                  <header>
                    <div class="container">
                      <div class="header-content">
                        <a href="/" class="logo">
                         
                        </a>
                        <nav>
                        
                        </nav>
                      </div>
                    </div>
                  </header>
                 
                  <div class="container">
                    <div class="main-content">
                      <div class="upload-form">
                        <h2 class="form-title">Error</h2>
                        <p style="margin-bottom: 20px; color: red;">""" + errorMessage + """
                        </p>
                        <a href="/upload" style="display: inline-block; width: 100%; text-align: center; background-color: var(--primary); color: white; padding: 12px 20px; text-decoration: none; border-radius: 4px;">Try Again</a>
                      </div>
                    </div>
                  </div>
                 
                  <footer>
                    <div class="container">
                      <p>&copy; 2025 BookFlick - Book Sharing Platform. All rights reserved.</p>
                    </div>
                  </footer>
                </body>
                </html>
                """;
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(500, errorPage.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorPage.getBytes());
        }
        }

        static class BookUploadProcessor implements HttpHandler {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendError(exchange, "Only POST method is supported");
                    return;
                }

                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                    sendError(exchange, "Expected multipart/form-data content type");
                    return;
                }

                // Extract boundary
                String boundary = null;
                String[] parts = contentType.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("boundary=")) {
                        boundary = part.substring("boundary=".length());
                        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                            boundary = boundary.substring(1, boundary.length() - 1);
                        }
                        break;
                    }
                }

                if (boundary == null) {
                    sendError(exchange, "Could not find boundary in content type");
                    return;
                }

                try {
                    // Read all form data into memory
                    InputStream inputStream = exchange.getRequestBody();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    byte[] requestData = baos.toByteArray();

                    // Process the multipart data
                    Map<String, String> formFields = new HashMap<>();
                    byte[] pdfData = null;

                    // Split by boundary
                    String data = new String(requestData, StandardCharsets.ISO_8859_1);
                    String[] dataItems = data.split("--" + boundary);

                    for (String item : dataItems) {
                        if (item.contains("Content-Disposition: form-data;")) {
                            // Get the name of the form field
                            String name = null;
                            boolean isFile = false;
                            String fileName = null;

                            int nameStart = item.indexOf("name=\"") + 6;
                            if (nameStart > 5) {
                                int nameEnd = item.indexOf("\"", nameStart);
                                name = item.substring(nameStart, nameEnd);
                            }

                            if (item.contains("filename=\"")) {
                                isFile = true;
                                int fileNameStart = item.indexOf("filename=\"") + 10;
                                int fileNameEnd = item.indexOf("\"", fileNameStart);
                                fileName = item.substring(fileNameStart, fileNameEnd);
                            }

                            // Extract the content after the headers
                            int contentStart = item.indexOf("\r\n\r\n") + 4;
                            if (contentStart > 3) {
                                String content = item.substring(contentStart);

                                if (isFile && "pdf".equals(name)) {
                                    // This is the PDF file
                                    int endIndex = content.lastIndexOf("\r\n");
                                    if (endIndex > 0) {
                                        content = content.substring(0, endIndex);
                                    }
                                    pdfData = content.getBytes(StandardCharsets.ISO_8859_1);
                                } else if (name != null) {
                                    // This is a regular form field
                                    int endIndex = content.lastIndexOf("\r\n");
                                    if (endIndex > 0) {
                                        content = content.substring(0, endIndex);
                                    }
                                    formFields.put(name, content);
                                }
                            }
                        }
                    }

                    // Process form fields
                    if (formFields.isEmpty() || pdfData == null || pdfData.length == 0) {
                        sendError(exchange, "Missing required form fields or PDF file");
                        return;
                    }

                    try {
                        // Extract fields
                        int bookId = Integer.parseInt(formFields.get("book_id"));
                        int stdId = Integer.parseInt(formFields.get("std_id"));
                        int semester = Integer.parseInt(formFields.get("semester"));
                        String bookName = formFields.get("book_name");
                        String stdName = formFields.get("std_name");
                        String type = formFields.get("type");
                        String subject = formFields.get("subject");
                        String courseCode = formFields.get("course_code");
                        String department = formFields.get("department");
                        String uploadYear = formFields.get("upload_year");

                        // Save PDF file
                        String fileName = "book_" + bookId + ".pdf";
                        String pdfFilePath = "uploads/" + fileName;
                        try (FileOutputStream fos = new FileOutputStream(pdfFilePath)) {
                            fos.write(pdfData);
                        }

                        // Insert into database
                        try {
                            Class.forName("com.mysql.cj.jdbc.Driver");
                            try (Connection conn = DriverManager.getConnection(
                                    "jdbc:mysql://localhost:3306/bookdb", "root", "root");
                                 PreparedStatement stmt = conn.prepareStatement(
                                         "INSERT INTO books (book_id, id, name, std_name, type, subject, course_code, department, semester, upload_year, pdf_path) " +
                                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                                stmt.setInt(1, bookId);
                                stmt.setInt(2, stdId);
                                stmt.setString(3, bookName);
                                stmt.setString(4, stdName);
                                stmt.setString(5, type);
                                stmt.setString(6, subject);
                                stmt.setString(7, courseCode);
                                stmt.setString(8, department);
                                stmt.setInt(9, semester);
                                stmt.setString(10, uploadYear);
                                stmt.setString(11, pdfFilePath);

                                int rows = stmt.executeUpdate();
                                if (rows > 0) {
                                    // Success - redirect to home page
                                    String successPage = """
                                            <!DOCTYPE html>
                                            <html lang="en">
                                            <head>
                                              <meta charset="UTF-8">
                                              <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                              <title>Success - StudyShare</title>
                                              <link rel="stylesheet" href="/css">
                                              <meta http-equiv="refresh" content="3;url=/" />
                                            </head>
                                            <body>
                                              <header>
                                                <div class="container">
                                                  <div class="header-content">
                                                    <a href="/" class="logo">
                                                      <h1>BookFlick</h1>
                                                    </a>
                                                    <nav>
                                            
                                                    </nav>
                                                  </div>
                                                </div>
                                              </header>
                                            
                                              <div class="container">
                                                <div class="main-content">
                                                  <div class="upload-form">
                                                    <h2 class="form-title">Upload Successful!</h2>
                                                    <p style="margin-bottom: 20px; color: green;">Your book has been uploaded successfully and will be available for others to view.</p>
                                                    <p>Redirecting to home page in 3 seconds...</p>
                                                    <p><a href="/" style="color: var(--primary);">Click here if you are not redirected automatically</a></p>
                                                  </div>
                                                </div>
                                              </div>
                                            
                                              <footer>
                                                <div class="container">
                                                  <p>&copy; BookFlick - Book Sharing Platform. All rights reserved.</p>
                                                </div>
                                              </footer>
                                            </body>
                                            </html>
                                            """;
                                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                                    exchange.sendResponseHeaders(200, successPage.getBytes().length);
                                    try (OutputStream os = exchange.getResponseBody()) {
                                        os.write(successPage.getBytes());
                                    }
                                } else {
                                    sendError(exchange, "Failed to insert book data into database");
                                }
                            }
                        } catch (ClassNotFoundException | SQLException e) {
                            sendError(exchange, "Database error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } catch (NumberFormatException e) {
                        sendError(exchange, "Invalid number format in form data: " + e.getMessage());
                    } catch (IOException e) {
                        sendError(exchange, "Error saving PDF file: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    sendError(exchange, "Error processing upload: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }}




