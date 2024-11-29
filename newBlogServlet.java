package com;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/blogServlet")
public class newBlogServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/blogging_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Set CORS headers
        setCorsHeaders(response);

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String category = request.getParameter("topic");
        String imageUrl = request.getParameter("image_url");

        Connection conn = null;
        PreparedStatement userStmt = null;
        PreparedStatement blogStmt = null;
        PreparedStatement imageStmt = null;

        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish database connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            //session code
            // Retrieve session and get user email
            HttpSession sess = request.getSession(false); // Retrieve existing session
            if (sess == null) {
                System.out.println("No active session. Redirecting to login.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{\"message\": \"Unauthorized access.\"}");
                return;
            }

            Object userEmailObj = sess.getAttribute("useremail");
            if (!(userEmailObj instanceof String)) {
                System.out.println("User email not found in session.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{\"message\": \"Unauthorized access.\"}");
                return;
            }
            
            String email = (String) userEmailObj;

            String selectUserSQL = "SELECT user_id FROM users WHERE email = ?";
            userStmt = conn.prepareStatement(selectUserSQL);
            userStmt.setString(1, email);

            ResultSet userResult = userStmt.executeQuery();
            if (!userResult.next()) {
                System.out.println("No user found for the provided email.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{\"message\": \"Unauthorized access.\"}");
                return;
            }

            int userId = userResult.getInt("user_id");

            // Insert blog details into blogs table
            String insertBlogSQL = "INSERT INTO blogs (user_id, blog_name, content, status, category) " +
                    "VALUES (?, ?, ?, ?, ?) RETURNING blog_id";
            blogStmt = conn.prepareStatement(insertBlogSQL);
            blogStmt.setInt(1, userId);
            blogStmt.setString(2, title);
            blogStmt.setString(3, content);
            blogStmt.setString(4, "Published"); // Assuming default status
            blogStmt.setString(5, category);

            ResultSet blogResult = blogStmt.executeQuery();
            if (blogResult.next()) {
                int blogId = blogResult.getInt("blog_id");

                // Insert image URL into blog_images table
                String insertImageSQL = "INSERT INTO blog_images (blog_id, image_url) VALUES (?, ?)";
                imageStmt = conn.prepareStatement(insertImageSQL);
                imageStmt.setInt(1, blogId);
                imageStmt.setString(2, imageUrl);
                imageStmt.executeUpdate();

                // Send response back to frontend with blog details
                response.setStatus(HttpServletResponse.SC_OK);
                out.println("{\"message\": \"Blog published successfully!\", " +
                        "\"blogId\": " + blogId + ", " +
                        "\"title\": \"" + title + "\", " +
                        "\"content\": \"" + content + "\", " +
                        "\"category\": \"" + category + "\", " +
                        "\"imageUrl\": \"" + imageUrl + "\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"message\": \"Failed to insert blog.\"}");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"message\": \"Error: PostgreSQL Driver not found.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"message\": \"Error while accessing the database.\"}");
        } finally {
            try {
                if (userStmt != null) userStmt.close();
                if (blogStmt != null) blogStmt.close();
                if (imageStmt != null) imageStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.close();
        }
    }

    // Handle CORS preflight requests
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // Method to set CORS headers
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, credentials"); // Add credentials here
        response.setHeader("Access-Control-Allow-Credentials", "true");  // Allow credentials
    }
}
