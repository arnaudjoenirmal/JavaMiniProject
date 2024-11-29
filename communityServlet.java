package com;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/communityServlet")
public class communityServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/blogging_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String username = request.getParameter("username"); // Get username from request
        String category = request.getParameter("category"); // Get category from request
        System.out.println("Received username: " + username);
        System.out.println("Received category: " + category);

        Connection conn = null;

        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish database connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String blogQuery;
            PreparedStatement blogStmt;

            // If username is provided, fetch blogs for that user
            if (username != null && !username.trim().isEmpty()) {
                // Query to fetch user_id based on the username
                String userQuery = "SELECT user_id FROM users WHERE username = ?";
                PreparedStatement userStmt = conn.prepareStatement(userQuery);
                userStmt.setString(1, username);
                ResultSet userRs = userStmt.executeQuery();

                int userId = -1;
                if (userRs.next()) {
                    userId = userRs.getInt("user_id");
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("{\"error\": \"User not found.\"}");
                    return;
                }

                // Query to fetch blogs related to the user_id
                blogQuery = """
                        SELECT b.blog_id, b.blog_name, b.content, b.status, b.category, b.created_at, i.image_url
                        FROM blogs b
                        LEFT JOIN blog_images i ON b.blog_id = i.blog_id
                        WHERE b.user_id = ? AND b.status = 'Published'
                        """;

                blogStmt = conn.prepareStatement(blogQuery);
                blogStmt.setInt(1, userId);
            } 
            // If category is provided, fetch blogs for that category
            else if (category != null && !category.trim().isEmpty()) {
                blogQuery = """
                        SELECT b.blog_id, b.blog_name, b.content, b.status, b.category, b.created_at, i.image_url
                        FROM blogs b
                        LEFT JOIN blog_images i ON b.blog_id = i.blog_id
                        WHERE b.category = ? AND b.status = 'Published'
                        """;

                blogStmt = conn.prepareStatement(blogQuery);
                blogStmt.setString(1, category);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"error\": \"No valid username or category provided.\"}");
                return;
            }

            ResultSet blogRs = blogStmt.executeQuery();

            // Build JSON response
            StringBuilder json = new StringBuilder();
            json.append("[");

            while (blogRs.next()) {
                if (json.length() > 1) json.append(",");
                json.append("{");
                json.append("\"blog_id\": \"").append(blogRs.getInt("blog_id")).append("\",");
                json.append("\"blog_name\": \"").append(blogRs.getString("blog_name")).append("\",");
                json.append("\"content\": \"").append(blogRs.getString("content")).append("\",");
                json.append("\"status\": \"").append(blogRs.getString("status")).append("\",");
                json.append("\"category\": \"").append(blogRs.getString("category")).append("\",");
                json.append("\"created_at\": \"").append(blogRs.getTimestamp("created_at")).append("\",");
                json.append("\"image_url\": \"").append(blogRs.getString("image_url")).append("\"");
                json.append("}");
            }
            json.append("]");

            response.setStatus(HttpServletResponse.SC_OK);
            out.println(json.toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"error\": \"Error: PostgreSQL Driver not found.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"error\": \"Error while accessing the database.\"}");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            out.close();
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
