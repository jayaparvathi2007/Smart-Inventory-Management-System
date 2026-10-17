package com.smartinventory.servlet;

import com.smartinventory.dao.TransactionDAO;
import com.smartinventory.model.InventoryTransaction;
import com.smartinventory.model.User;
import com.smartinventory.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/stock")
public class StockServlet extends HttpServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String productIdParam = req.getParameter("productId");
        String type = req.getParameter("type");
        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");
        String limitParam = req.getParameter("limit");

        Integer productId = null;
        if (productIdParam != null && !productIdParam.trim().isEmpty() && !"ALL".equalsIgnoreCase(productIdParam.trim())) {
            try {
                productId = Integer.parseInt(productIdParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        int limit = 100;
        if (limitParam != null && !limitParam.trim().isEmpty()) {
            try {
                limit = Integer.parseInt(limitParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        try {
            List<InventoryTransaction> list = transactionDAO.getTransactions(productId, type, startDate, endDate, limit);
            JsonUtil.sendSuccess(resp, "Transaction history retrieved successfully", list);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = JsonUtil.readRequestBody(req);
        Map<?, ?> map = JsonUtil.fromJson(body, Map.class);

        if (map == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid stock payload.");
            return;
        }

        String action = (String) map.get("action");
        if (action == null || action.trim().isEmpty()) {
            action = req.getParameter("action");
        }

        if (action == null || (!action.equalsIgnoreCase("in") && !action.equalsIgnoreCase("out"))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid action. Use 'in' or 'out'.");
            return;
        }

        // Parse fields
        int productId;
        int quantity;
        BigDecimal unitPrice = BigDecimal.ZERO;
        String referenceNo = (String) map.get("referenceNo");
        String notes = (String) map.get("notes");

        try {
            Object pidObj = map.get("productId");
            if (pidObj instanceof Number) {
                productId = ((Number) pidObj).intValue();
            } else {
                productId = Integer.parseInt(pidObj.toString());
            }

            Object qtyObj = map.get("quantity");
            if (qtyObj instanceof Number) {
                quantity = ((Number) qtyObj).intValue();
            } else {
                quantity = Integer.parseInt(qtyObj.toString());
            }

            Object priceObj = map.get("unitPrice");
            if (priceObj != null) {
                if (priceObj instanceof Number) {
                    unitPrice = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                } else if (!priceObj.toString().trim().isEmpty()) {
                    unitPrice = new BigDecimal(priceObj.toString().trim());
                }
            }
        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID, quantity, or price format.");
            return;
        }

        if (productId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid product selection is required.");
            return;
        }
        if (quantity <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Quantity must be greater than 0.");
            return;
        }

        // Get current user ID from session
        HttpSession session = req.getSession(false);
        Integer userId = null;
        if (session != null && session.getAttribute("user") != null) {
            userId = ((User) session.getAttribute("user")).getId();
        }

        try {
            if ("in".equalsIgnoreCase(action)) {
                transactionDAO.recordStockIn(productId, quantity, unitPrice, referenceNo, notes, userId);
                JsonUtil.sendSuccess(resp, "Stock-In recorded successfully! " + quantity + " units added.");
            } else {
                transactionDAO.recordStockOut(productId, quantity, unitPrice, referenceNo, notes, userId);
                JsonUtil.sendSuccess(resp, "Stock-Out recorded successfully! " + quantity + " units deducted.");
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
