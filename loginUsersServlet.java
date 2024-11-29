package com;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
//import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
@WebServlet("/login")
public class loginUsersServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/blogging_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Set CORS headers
        setCorsHeaders(response);

        response.setContentType("application/json");  //content type is set to the json.
        
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        Connection conn = null;
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");

            // Establish database connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // SQL statement to check user
            String userCheckQuery = "SELECT * FROM users WHERE email = ?";
            PreparedStatement statement = conn.prepareStatement(userCheckQuery);
            statement.setString(1, email);

            // Execute the query
            ResultSet ret = statement.executeQuery();

            if (ret.next()) {
                // Checking if the password matches
                String databasePassword = ret.getString("password");
                if (databasePassword.equals(password)) {
                    // Successful login
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.println("{\"message\": \"Login Successful\"}");
                    
                    //session code.
                    HttpSession usersession = request.getSession(true); // Create new session if none exists
                    if (email != null) {
                        usersession.setAttribute("useremail", email);
                        System.out.println("Session ID at login: " + usersession.getId());// Store email in session
                        System.out.println("Setting session with email: " + usersession.getAttribute("useremail"));
                        System.out.println("Response Set-Cookie header: " + response.getHeader("Set-Cookie"));
                    } else {
                        throw new IllegalArgumentException("Email cannot be null.");
                    }
                    


                } else {
                    // Invalid password
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.println("{\"message\": \"Incorrect password\"}");
                }
            } else {
                // User not found
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{\"message\": \"User not found\"}");
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
            // Close resources
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
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, credentials"); // Add credentials here
        response.setHeader("Access-Control-Allow-Credentials", "true");  // Allow credentials
    }

}
