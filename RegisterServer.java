package com.bookshare;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.sql.*;

public class RegisterServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9091), 0);
        server.createContext("/register", new RegisterFormHandler());
        server.createContext("/submit-registration", new RegisterHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Registration Server started at http://localhost:9091/register");
    }

    // Serves the registration form
    static class RegisterFormHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String registerForm =
                    "<!DOCTYPE html>\n" +
                            "<html lang=\"en\">\n" +
                            "<head>\n" +
                            "    <meta charset=\"UTF-8\">\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                            "    <title>Register</title>\n" +
                            "    <style>\n" +
                            "        body {\n" +
                            "            margin: 0;\n" +
                            "            font-family: Arial, sans-serif;\n" +
                            "            background-color: #f0f4f8;\n" +
                            "            display: flex;\n" +
                            "            justify-content: center;\n" +
                            "            align-items: center;\n" +
                            "            height: 100vh;\n" +
                            "        }\n" +
                            "        .container {\n" +
                            "            background-color: white;\n" +
                            "            padding: 30px;\n" +
                            "            border-radius: 10px;\n" +
                            "            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);\n" +
                            "            width: 100%;\n" +
                            "            max-width: 400px;\n" +
                            "        }\n" +
                            "        h2 {\n" +
                            "            text-align: center;\n" +
                            "            color: #0a1172;\n" +
                            "        }\n" +
                            "        form div {\n" +
                            "            margin-bottom: 15px;\n" +
                            "        }\n" +
                            "        label {\n" +
                            "            display: block;\n" +
                            "            margin-bottom: 5px;\n" +
                            "            color: #0a1172;\n" +
                            "            font-weight: bold;\n" +
                            "        }\n" +
                            "        input[type=\"text\"], input[type=\"email\"], input[type=\"password\"], input[type=\"number\"] {\n" +
                            "            width: 100%;\n" +
                            "            padding: 10px;\n" +
                            "            border: 1px solid #ccc;\n" +
                            "            border-radius: 5px;\n" +
                            "        }\n" +
                            "        button {\n" +
                            "            width: 100%;\n" +
                            "            padding: 10px;\n" +
                            "            background-color: #0a1172;\n" +
                            "            color: white;\n" +
                            "            border: none;\n" +
                            "            border-radius: 5px;\n" +
                            "            font-size: 16px;\n" +
                            "            cursor: pointer;\n" +
                            "        }\n" +
                            "        button:hover {\n" +
                            "            background-color: #09105d;\n" +
                            "        }\n" +
                            "        .login-link {\n" +
                            "            text-align: center;\n" +
                            "            margin-top: 15px;\n" +
                            "        }\n" +
                            "        .login-link a {\n" +
                            "            color: #0a1172;\n" +
                            "            text-decoration: none;\n" +
                            "        }\n" +
                            "        .login-link a:hover {\n" +
                            "            text-decoration: underline;\n" +
                            "        }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <div class=\"container\">\n" +
                            "        <h2>Register</h2>\n" +
                            "        <form action=\"/submit-registration\" method=\"post\">\n" +
                            "            <div>\n" +
                            "                <label>ID:</label>\n" +
                            "                <input type=\"number\" name=\"id\" required>\n" +
                            "            </div>\n" +
                            "            <div>\n" +
                            "                <label>Name:</label>\n" +
                            "                <input type=\"text\" name=\"name\" required>\n" +
                            "            </div>\n" +
                            "            <div>\n" +
                            "                <label>Email:</label>\n" +
                            "                <input type=\"email\" name=\"email\" required>\n" +
                            "            </div>\n" +
                            "            <div>\n" +
                            "                <label>Password:</label>\n" +
                            "                <input type=\"password\" name=\"password\" required>\n" +
                            "            </div>\n" +
                            "            <button type=\"submit\">Register</button>\n" +
                            "        </form>\n" +
                            "        <div class=\"login-link\">\n" +
                            "            <p>Already have an account? <a href=\"http://localhost:9097/login\">Login</a></p>\n" +
                            "        </div>\n" +
                            "    </div>\n" +
                            "</body>\n" +
                            "</html>";

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, registerForm.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(registerForm.getBytes());
            os.close();
        }
    }

    // Handles registration form submission
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }

                // Parse form data
                String[] formData = requestBody.toString().split("&");
                int id = 0;
                String name = "", email = "", password = "";

                for (String pair : formData) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String key = URLDecoder.decode(keyValue[0], "UTF-8");
                        String value = URLDecoder.decode(keyValue[1], "UTF-8").replace("+", " ");
                        if (key.equals("id")) id = Integer.parseInt(value);
                        else if (key.equals("name")) name = value;
                        else if (key.equals("email")) email = value;
                        else if (key.equals("password")) password = value;
                    }
                }

                // Validate data
                if (id <= 0 || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    sendResponse(exchange, 400, "All fields are required");
                    return;
                }

                if (!email.endsWith("@mbcet.ac.in")) {
                    String errorHtml = "<html><body>" +
                            "<h2>Register</h2>" +
                            "<p style='color:red;'>Please enter a valid institutional email ending with @mbcet.ac.in</p>" +
                            "<a href=\"/register\">Back to Register</a>" +
                            "</body></html>";
                    sendResponse(exchange, 400, errorHtml);
                    return;
                }

                // Store in database
                Connection conn = null;
                PreparedStatement stmt = null;
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root");

                    String sql = "INSERT INTO login_info (id, name, email, password) VALUES (?, ?, ?, ?)";
                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, id);
                    stmt.setString(2, name);
                    stmt.setString(3, email);
                    stmt.setString(4, password);

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        // Redirect to login page
                        exchange.getResponseHeaders().set("Location", "http://localhost:9097/login");
                        exchange.sendResponseHeaders(302, -1);
                    } else {
                        sendResponse(exchange, 500, "Registration failed");
                    }

                } catch (SQLException | ClassNotFoundException e) {
                    String errMsg = e.getMessage();
                    if (errMsg.toLowerCase().contains("duplicate")) {
                        sendResponse(exchange, 400, "ID or Email already registered");
                    } else {
                        sendResponse(exchange, 500, "Error: " + errMsg);
                    }
                } finally {
                    try {
                        if (stmt != null) stmt.close();
                        if (conn != null) conn.close();
                    } catch (SQLException ignore) {}
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(statusCode, message.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(message.getBytes());
            os.close();
        }
    }
}
