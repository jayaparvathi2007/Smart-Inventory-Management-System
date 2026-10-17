package com.smartinventory.servlet;

import com.smartinventory.dao.DashboardDAO;
import com.smartinventory.model.DashboardSummary;
import com.smartinventory.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    private final DashboardDAO dashboardDAO = new DashboardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            DashboardSummary summary = dashboardDAO.getDashboardSummary();
            JsonUtil.sendSuccess(resp, "Dashboard data loaded successfully", summary);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error loading dashboard: " + e.getMessage());
        }
    }
}
