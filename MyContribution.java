package com.bookshare;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class MyContribution {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(9099), 0);
        server.createContext("/", new PointsHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started at http://localhost:9099/");
    }

    static class PointsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            Map<String, String> queryParams = parseQuery(query);

            String studentIdStr = queryParams.get("id");
            String htmlContent;

            if (studentIdStr != null) {
                htmlContent = getRatingsForUser(studentIdStr);
            } else {
                htmlContent = "<html><body><h1>Please specify a student ID in the URL (e.g., /?id=1)</h1></body></html>";
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, htmlContent.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(htmlContent.getBytes());
            }
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> queryParams = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        queryParams.put(kv[0], kv[1]);
                    }
                }
            }
            return queryParams;
        }

        private String getRatingsForUser(String studentIdStr) {
            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<!DOCTYPE html>")
                    .append("<html lang=\"en\">")
                    .append("<head>")
                    .append("<meta charset=\"UTF-8\">")
                    .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                    .append("<title>Ratings for Student ").append(studentIdStr).append("</title>")
                    .append("<link href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap\" rel=\"stylesheet\">")
                    .append("<style>")
                    .append("body { font-family: 'Poppins', sans-serif; background-color: #f5f7fa; padding: 30px; margin: 0; color: #333; }")
                    .append("h1 { text-align: center; color: #000d56; font-size: 2.5rem; margin-bottom: 20px; font-weight: 600; }")
                    .append(".container { max-width: 1100px; margin: 0 auto; background-color: #fff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }")
                    .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
                    .append("table, th, td { border: 1px solid #ddd; }")
                    .append("th, td { padding: 12px 20px; text-align: center; font-size: 1rem; }")
                    .append("th { background-color: #3d4885; color: white; font-weight: 600; text-transform: uppercase; }")
                    .append("tr:nth-child(even) { background-color: #f9fafb; }")
                    .append("tr:hover { background-color: #e1efff; cursor: pointer; transition: background-color 0.3s ease; }")
                    .append("td { color: #333; }")
                    .append(".footer { text-align: center; font-size: 0.9rem; color: #777; margin-top: 40px; }")
                    .append("@media (max-width: 768px) {")
                    .append("    body { padding: 15px; }")
                    .append("    h1 { font-size: 2rem; }")
                    .append("    table, th, td { font-size: 0.9rem; padding: 10px; }")
                    .append("}")
                    .append("</style>")
                    .append("</head>")
                    .append("<body>")
                    .append("<div class=\"container\">")
                    .append("<h1>Ratings for Student ID: ").append(studentIdStr).append("</h1>")
                    .append("<table>")
                    .append("<thead>")
                    .append("<tr><th>Student ID</th><th>R</th><th>N</th><th>GP</th></tr>")
                    .append("</thead>")
                    .append("<tbody>");

            // Connect to DB and fetch ratings
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bookdb", "root", "root");
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id, R, N, GP FROM rating_values WHERE id = ?")) {

                stmt.setInt(1, Integer.parseInt(studentIdStr));
                ResultSet rs = stmt.executeQuery();

                boolean hasResult = false;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    float r = rs.getFloat("R");
                    float n = rs.getFloat("N");
                    float gp = rs.getFloat("GP");

                    htmlContent.append("<tr>")
                            .append("<td>").append(id).append("</td>")
                            .append("<td>").append(r).append("</td>")
                            .append("<td>").append(n).append("</td>")
                            .append("<td>").append(gp).append("</td>")
                            .append("</tr>");
                    hasResult = true;
                }

                if (!hasResult) {
                    htmlContent.append("<tr><td colspan=\"4\">No ratings found for this student.</td></tr>");
                }

            } catch (SQLException e) {
                htmlContent.append("<tr><td colspan=\"4\">Error accessing database: ")
                        .append(e.getMessage()).append("</td></tr>");
            }

            htmlContent.append("</tbody>")
                    .append("</table>")
                    .append("<div class=\"footer\">")
                    .append("<p>&copy; 2025 Book Ratings System. All rights reserved.</p>")
                    .append("</div>")
                    .append("</div>")
                    .append("</body>")
                    .append("</html>");

            return htmlContent.toString();
        }
    }
}
