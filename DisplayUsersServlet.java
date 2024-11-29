package com;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/register")
public class DisplayUsersServlet extends HttpServlet {
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

        // Retrieve query parameters from the URL (e.g., /register?username=xxx&email=xxx&password=xxx&profession=xxx)
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String profession = request.getParameter("profession");

        Connection conn = null;
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish database connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // SQL statement to insert a new user
            String insertQuery = "INSERT INTO users (email, username, profession, password) VALUES (?, ?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(insertQuery);
            statement.setString(1, email);
            statement.setString(2, username);
            statement.setString(3, profession);
            statement.setString(4, password);

            // Execute the insertion
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                // Successful registration
                out.println("{\"message\": \"Registration Successful!\"}");
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                // Failed registration
                out.println("{\"message\": \"Registration Failed. Please try again.\"}");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            out.println("{\"message\": \"Error: PostgreSQL Driver not found.\"}");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("{\"message\": \"Error while accessing the database.\"}");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            // Close resources
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            // Close the writer
            out.close();
        }
    }

    // Handle CORS preflight requests
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // Method to set CORS headers
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
