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
@WebServlet("/dashboardServlet")
public class dashboardServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/blogging_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Connection conn = null;

        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish database connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

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

            // Retrieve user_id based on email
            String userIdQuery = "SELECT user_id FROM users WHERE email = ?";
            PreparedStatement userIdStmt = conn.prepareStatement(userIdQuery);
            userIdStmt.setString(1, email);
            ResultSet userIdRs = userIdStmt.executeQuery();

            if (!userIdRs.next()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{\"message\": \"Unauthorized user.\"}");
                return;
            }

            int userId = userIdRs.getInt("user_id");

            // Query to fetch published blogs authored by the user
            String blogQuery = """
                    SELECT b.blog_id, b.blog_name, b.content, b.status, b.category, b.created_at, i.image_url
                    FROM blogs b
                    LEFT JOIN blog_images i ON b.blog_id = i.blog_id
                    WHERE b.status = 'Published' AND b.user_id = ?
                    """;

            PreparedStatement blogStmt = conn.prepareStatement(blogQuery);
            blogStmt.setInt(1, userId);
            ResultSet blogRs = blogStmt.executeQuery();

            // Build JSON response
            StringBuilder json = new StringBuilder();
            json.append("[");

            while (blogRs.next()) {
                if (json.length() > 1) json.append(",");

                int blogId = blogRs.getInt("blog_id");
                String likeAndCommentQuery = """
                        WITH like_count AS (
                            SELECT COUNT(*) AS total_likes
                            FROM blog_likes
                            WHERE blog_id = ? AND comment IS NULL
                        ),
                        comments AS (
                            SELECT u.username, bl.comment, bl.liked_at
                            FROM blog_likes bl
                            JOIN users u ON bl.user_id = u.user_id
                            WHERE bl.blog_id = ? AND bl.comment IS NOT NULL
                        )
                        SELECT 
                            (SELECT total_likes FROM like_count) AS like_count,
                            c.username,
                            c.comment,
                            c.liked_at
                        FROM comments c;
                        """;

                PreparedStatement likeAndCommentStmt = conn.prepareStatement(likeAndCommentQuery);
                likeAndCommentStmt.setInt(1, blogId);
                likeAndCommentStmt.setInt(2, blogId);
                ResultSet lcRs = likeAndCommentStmt.executeQuery();

                // Build likes and comments JSON
                StringBuilder likesCommentsJson = new StringBuilder();
                int likeCount = 0;
                likesCommentsJson.append("[");
                while (lcRs.next()) {
                    if (lcRs.isFirst()) {
                        likeCount = lcRs.getInt("like_count");
                    }
                    if (likesCommentsJson.length() > 1) likesCommentsJson.append(",");
                    likesCommentsJson.append("{");
                    likesCommentsJson.append("\"username\": \"").append(lcRs.getString("username")).append("\",");
                    likesCommentsJson.append("\"comment\": \"").append(lcRs.getString("comment")).append("\",");
                    likesCommentsJson.append("\"liked_at\": \"").append(lcRs.getTimestamp("liked_at")).append("\"");
                    likesCommentsJson.append("}");
                }
                likesCommentsJson.append("]");

                json.append("{");
                json.append("\"blog_id\": \"").append(blogId).append("\",");
                json.append("\"blog_name\": \"").append(blogRs.getString("blog_name")).append("\",");
                json.append("\"content\": \"").append(blogRs.getString("content")).append("\",");
                json.append("\"status\": \"").append(blogRs.getString("status")).append("\",");
                json.append("\"category\": \"").append(blogRs.getString("category")).append("\",");
                json.append("\"created_at\": \"").append(blogRs.getTimestamp("created_at")).append("\",");
                json.append("\"image_url\": \"").append(blogRs.getString("image_url")).append("\",");
                json.append("\"like_count\": ").append(likeCount).append(",");
                json.append("\"comments\": ").append(likesCommentsJson);
                json.append("}");
            }
            json.append("]");

            response.setStatus(HttpServletResponse.SC_OK);
            out.println(json.toString());

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"message\": \"Error: PostgreSQL Driver not found.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"message\": \"Error while accessing the database.\"}");
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
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, credentials");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
