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

@WebServlet("/commentServlet")
public class commentServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/blogging_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        int blogId = Integer.parseInt(request.getParameter("blog_id"));
        String commentText = request.getParameter("comment");

        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement userStmt = null;

        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish database connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Session handling: retrieve user id from session
            HttpSession sess = request.getSession(false); // Retrieve existing session
            if (sess == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{\"message\": \"Unauthorized access.\"}");
                return;
            }

            Object userEmailObj = sess.getAttribute("useremail");
            if (!(userEmailObj instanceof String)) {
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
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{\"message\": \"Unauthorized access.\"}");
                return;
            }

            int userId = userResult.getInt("user_id");

            // Insert comment into blog_likes table (same table as like, but with comment)
            String query = "INSERT INTO blog_likes (blog_id, user_id, comment) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, blogId);
            stmt.setInt(2, userId);
            stmt.setString(3, commentText);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
            	System.out.println("rows has been updated");
            }else {            	
            	System.out.println("rows has not been updated");
            }

            response.setStatus(HttpServletResponse.SC_OK);
            out.println("{\"message\": \"Comment added successfully.\"}");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"message\": \"Error processing comment.\"}");
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
