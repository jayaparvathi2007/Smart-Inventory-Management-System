package com.smartinventory.servlet;

import com.smartinventory.dao.UserDAO;
import com.smartinventory.model.User;
import com.smartinventory.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth")
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("logout".equalsIgnoreCase(action)) {
            handleLogout(req, resp);
            return;
        }

        // Check current session
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("fullName", user.getFullName());
            userData.put("role", user.getRole());
            JsonUtil.sendSuccess(resp, "Authenticated", userData);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("logout".equalsIgnoreCase(action)) {
            handleLogout(req, resp);
            return;
        }

        // Login handling
        String body = JsonUtil.readRequestBody(req);
        Map<?, ?> credentials = JsonUtil.fromJson(body, Map.class);
        
        String username = null;
        String password = null;

        if (credentials != null) {
            username = (String) credentials.get("username");
            password = (String) credentials.get("password");
        }

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        try {
            User user = userDAO.authenticate(username.trim(), password.trim());
            if (user != null) {
                HttpSession session = req.getSession(true);
                session.setAttribute("user", user);
                session.setMaxInactiveInterval(3600); // 1 hour session

                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("username", user.getUsername());
                userData.put("fullName", user.getFullName());
                userData.put("role", user.getRole());
                JsonUtil.sendSuccess(resp, "Login successful", userData);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during login: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.sendSuccess(resp, "Logged out successfully");
    }
}
