package com.bookshare;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class LoginServer implements HttpHandler {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/bookdb";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9097), 0);
        server.createContext("/login", new LoginServer());
        server.setExecutor(null);
        server.start();
        System.out.println("LoginServer started at http://localhost:9097/login");
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String formData = reader.readLine();
            String[] pairs = formData.split("&");

            String username = "", password = "";
            int studentId = -1;

            for (String pair : pairs) {
                String[] keyVal = pair.split("=");
                if (keyVal.length < 2) continue;
                String key = URLDecoder.decode(keyVal[0], StandardCharsets.UTF_8);
                String val = URLDecoder.decode(keyVal[1], StandardCharsets.UTF_8).replace("+", " ");
                switch (key) {
                    case "username" -> username = val;
                    case "password" -> password = val;
                    case "studentid" -> {
                        try {
                            studentId = Integer.parseInt(val);
                        } catch (NumberFormatException e) {
                            studentId = -1;
                        }
                    }
                }
            }

            String response;
            if (validateLogin(username, password, studentId)) {
                String redirectUrl = "http://localhost:9095/books?id=" + studentId;
                String redirectUrl2 = "http://localhost:9090/?id=" + studentId;
                String redirectUrl3 = "http://localhost:9099/?id=" + studentId;
                String redirectUrl4 = "http://localhost:9096/?id=" + studentId;


                response = "<html>"
                        + "<head>"
                        + "<meta http-equiv=\"refresh\" content=\"0; URL='" + redirectUrl + "'\" />"
                        + "</head>"
                        + "<body>"
                        + "<p style='text-align:center;'>Login successful! Redirecting to resources...</p>"
                        + "</body>"
                        + "</html>";
                response = "<html>"
                        + "<head>"
                        + "<meta http-equiv=\"refresh\" content=\"0; URL='" + redirectUrl4 + "'\" />"
                        + "</head>"
                        + "<body>"
                        + "<p style='text-align:center;'>Login successful! Redirecting to resources...</p>"
                        + "</body>"
                        + "</html>";

            } else {
                response = "<h2 style='color:red;text-align:center;'>Invalid credentials or ID. Please try again.</h2>";
                response += getLoginForm();
            }

            sendResponse(exchange, response);
        } else {
            sendResponse(exchange, getLoginForm());
        }
    }

    private boolean validateLogin(String username, String password, int studentId) {
        if (studentId <= 0 || username.isEmpty() || password.isEmpty()) return false;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String query = "SELECT * FROM login_info WHERE id = ? AND name = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, studentId);
                stmt.setString(2, username);
                stmt.setString(3, password);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String getLoginForm() {
        return """
                  <!DOCTYPE html>
                      <html lang="en">
                      <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>Login - BookFlick</title>
                          <style>
                              @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap');
                
                              body {
                                  margin: 0;
                                  padding: 0;
                                  min-height: 100vh;
                                  background: linear-gradient(135deg, #001f3f, #003366);
                                  font-family: 'Poppins', sans-serif;
                                  display: flex;
                                  flex-direction: column;
                                  align-items: center;
                                  justify-content: center;
                                  overflow: hidden;
                              }
                              .background {
                                  position: absolute;
                                  top: 0; left: 0;
                                  width: 100%;
                                  height: 100%;
                                  background: url('https://www.transparenttextures.com/patterns/cubes.png');
                                  opacity: 0.05;
                                  z-index: -1;
                                  animation: move 30s linear infinite;
                              }
                              @keyframes move {
                                  0% { background-position: 0 0; }
                                  100% { background-position: 1000px 1000px; }
                              }
                              .logo {
                                  font-size: 48px;
                                  font-weight: 700;
                                  color: #fff;
                                  text-align: center;
                                  margin-bottom: 20px;
                                  letter-spacing: 2px;
                                  background: linear-gradient(to right, #ffffff, #dce7f5);
                                  -webkit-background-clip: text;
                                  -webkit-text-fill-color: transparent;
                                  animation: slideDown 1s ease-out;
                              }
                              @keyframes slideDown {
                                  0% { opacity: 0; transform: translateY(-50px); }
                                  100% { opacity: 1; transform: translateY(0); }
                              }
                              .underline {
                                  width: 80px;
                                  height: 4px;
                                  background: #ffffff;
                                  margin: 8px auto 20px;
                                  border-radius: 2px;
                                  margin-top: -25px;
                              }
                             \s
                              /* Enhanced Login Container Styles */
                              .login-container {
                                  background: #fff;
                                  padding: 40px 35px;
                                  border-radius: 20px;
                                  box-shadow: 0 15px 30px rgba(0,0,0,0.3), 0 5px 15px rgba(0,0,0,0.2);
                                  width: 100%;
                                  max-width: 400px;
                                  animation: fadeIn 1s ease-in-out;
                                  position: relative;
                                  overflow: hidden;
                              }
                             \s
                              .login-container::before {
                                  content: '';
                                  position: absolute;
                                  top: 0;
                                  left: 0;
                                  right: 0;
                                  height: 6px;
                                  background: linear-gradient(to right, #001f3f, #0066cc);
                                  border-radius: 20px 20px 0 0;
                              }
                             \s
                              @keyframes fadeIn {
                                  0% { opacity: 0; transform: translateY(30px); }
                                  100% { opacity: 1; transform: translateY(0); }
                              }
                             \s
                              .login-container h2 {
                                  text-align: center;
                                  color: #001f3f;
                                  margin-bottom: 30px;
                                  font-size: 28px;
                                  font-weight: 700;
                                  position: relative;
                                  padding-bottom: 12px;
                              }
                             \s
                              .login-container h2::after {
                                  content: '';
                                  position: absolute;
                                  bottom: 0;
                                  left: 50%;
                                  transform: translateX(-50%);
                                  width: 50px;
                                  height: 3px;
                                  background: linear-gradient(to right, #001f3f, #0066cc);
                                  border-radius: 2px;
                              }
                             \s
                              .form-group {
                                  margin-bottom: 25px;
                                  position: relative;
                              }
                             \s
                              .form-group input {
                                  width: 90%;
                                  padding: 15px 15px 15px 45px;
                                  border: 2px solid #e0e0e0;
                                  border-radius: 12px;
                                  font-size: 15px;
                                  transition: all 0.3s ease;
                                  background: #f9f9f9;
                                  font-family: 'Poppins', sans-serif;
                              }
                             \s
                              .form-group input:focus {
                                  border-color: #0066cc;
                                  box-shadow: 0 0 0 3px rgba(0, 31, 63, 0.1);
                                  outline: none;
                                  background: #fff;
                              }
                             \s
                              .form-group .icon {
                                  position: absolute;
                                  left: 15px;
                                  top: 50%;
                                  transform: translateY(-50%);
                                  font-size: 18px;
                                  color: #666;
                                  transition: color 0.3s;
                              }
                             \s
                              .form-group:focus-within .icon {
                                  color: #0066cc;
                              }
                             \s
                              .checkbox-container {
                                  display: flex;
                                  align-items: center;
                                  margin-bottom: 25px;
                              }
                             \s
                              .checkbox-container input[type="checkbox"] {
                                  width: 18px;
                                  height: 18px;
                                  margin-right: 10px;
                                  accent-color: #0066cc;
                                  cursor: pointer;
                              }
                             \s
                              .checkbox-container label {
                                  font-size: 14px;
                                  color: #444;
                                  cursor: pointer;
                                  font-weight: 500;
                              }
                             \s
                              .forgot-password {
                                  text-align: right;
                                  margin-top: -15px;
                                  margin-bottom: 20px;
                              }
                             \s
                              .forgot-password a {
                                  font-size: 13px;
                                  color: #666;
                                  text-decoration: none;
                                  transition: color 0.3s;
                              }
                             \s
                              .forgot-password a:hover {
                                  color: #0066cc;
                              }
                             \s
                              .btn {
                                  width: 100%;
                                  background: linear-gradient(to right, #001f3f, #0066cc);
                                  color: #fff;
                                  padding: 16px;
                                  font-size: 16px;
                                  border: none;
                                  border-radius: 12px;
                                  cursor: pointer;
                                  transition: all 0.3s;
                                  font-weight: 600;
                                  box-shadow: 0 4px 10px rgba(0, 31, 63, 0.2);
                                  position: relative;
                                  overflow: hidden;
                                  font-family: 'Poppins', sans-serif;
                              }
                             \s
                              .btn::before {
                                  content: '';
                                  position: absolute;
                                  top: 0;
                                  left: -100%;
                                  width: 100%;
                                  height: 100%;
                                  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
                                  transition: all 0.6s;
                              }
                             \s
                              .btn:hover {
                                  transform: translateY(-3px);
                                  box-shadow: 0 6px 15px rgba(0, 31, 63, 0.3);
                              }
                             \s
                              .btn:hover::before {
                                  left: 100%;
                              }
                             \s
                              .links {
                                  margin-top: 30px;
                                  text-align: center;
                                  font-size: 14px;
                                  color: #555;
                              }
                             \s
                              .links a {
                                  color: #0066cc;
                                  text-decoration: none;
                                  font-weight: 600;
                                  transition: color 0.3s;
                                  position: relative;
                              }
                             \s
                              .links a::after {
                                  content: '';
                                  position: absolute;
                                  bottom: -2px;
                                  left: 0;
                                  width: 0;
                                  height: 2px;
                                  background: #0066cc;
                                  transition: width 0.3s;
                              }
                             \s
                              .links a:hover {
                                  color: #001f3f;
                              }
                             \s
                              .links a:hover::after {
                                  width: 100%;
                              }
                             \s
                              @media (max-width: 480px) {
                                  .login-container {
                                      padding: 30px 25px;
                                      margin: 0 15px;
                                  }
                              }
                          </style>
                      </head>
                      <body>
                          <div class="background"></div>
                
                          <div class="logo">BookFlick</div>
                          <div class="underline"></div>
                
                          <div class="login-container">
                              <h2>Student Login</h2>
                              <form action="/login" method="post">
                                  <div class="form-group">
                                      <span class="icon">🆔</span>
                                      <input type="number" id="studentid" name="studentid" placeholder="Enter your Student ID" required>
                                  </div>
                                  <div class="form-group">
                                      <span class="icon">👤</span>
                                      <input type="text" id="username" name="username" placeholder="Enter your Username" required>
                                  </div>
                                  <div class="form-group">
                                      <span class="icon">🔒</span>
                                      <input type="password" id="password" name="password" placeholder="Enter your Password" required>
                                  </div>
                                  <div class="checkbox-container">
                                      <input type="checkbox" id="remember" name="remember">
                                      <label for="remember">Remember me</label>
                                  </div>
                                  <div class="forgot-password">
                                      <a href="#">Forgot password?</a>
                                  </div>
                                  <button type="submit" class="btn">Login</button>
                              </form>
                              <div class="links">
                                  <p>Don't have an account? <a href="http://localhost:9091/register">Sign Up</a></p>
                              </div>
                          </div>
                      </body>
                      </html>
                
                
            """;
    }
}
