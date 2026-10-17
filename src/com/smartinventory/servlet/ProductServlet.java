package com.smartinventory.servlet;

import com.smartinventory.dao.ProductDAO;
import com.smartinventory.dao.TransactionDAO;
import com.smartinventory.model.Product;
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

@WebServlet("/api/products")
public class ProductServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        String action = req.getParameter("action");

        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                int id = Integer.parseInt(idParam.trim());
                Product product = productDAO.getProductById(id);
                if (product != null) {
                    JsonUtil.sendSuccess(resp, "Product found", product);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found with ID: " + id);
                }
            } else if ("low-stock".equalsIgnoreCase(action)) {
                List<Product> list = productDAO.getLowStockProducts();
                JsonUtil.sendSuccess(resp, "Low stock products retrieved", list);
            } else {
                String query = req.getParameter("query");
                String catParam = req.getParameter("categoryId");
                String stockStatus = req.getParameter("stockStatus");

                Integer categoryId = null;
                if (catParam != null && !catParam.trim().isEmpty() && !"ALL".equalsIgnoreCase(catParam.trim())) {
                    try {
                        categoryId = Integer.parseInt(catParam.trim());
                    } catch (NumberFormatException ignored) {}
                }

                List<Product> list = productDAO.searchProducts(query, categoryId, stockStatus);
                JsonUtil.sendSuccess(resp, "Products retrieved successfully", list);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric parameter format.");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = JsonUtil.readRequestBody(req);
        Product product = JsonUtil.fromJson(body, Product.class);

        if (product == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product payload.");
            return;
        }

        // Validation
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product SKU is required.");
            return;
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product name is required.");
            return;
        }
        if (product.getCategoryId() <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid category is required.");
            return;
        }
        if (product.getCostPrice() == null || product.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cost price must be non-negative.");
            return;
        }
        if (product.getSellingPrice() == null || product.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Selling price must be non-negative.");
            return;
        }
        if (product.getQuantity() < 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Initial quantity cannot be negative.");
            return;
        }

        try {
            // Check SKU uniqueness
            if (productDAO.existsBySku(product.getSku(), null)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Product with SKU '" + product.getSku() + "' already exists.");
                return;
            }

            int initialQty = product.getQuantity();
            boolean added = productDAO.addProduct(product);

            if (added) {
                // If initial quantity > 0, log an initial stock-in transaction
                if (initialQty > 0) {
                    HttpSession session = req.getSession(false);
                    Integer userId = null;
                    if (session != null && session.getAttribute("user") != null) {
                        userId = ((User) session.getAttribute("user")).getId();
                    }
                    try {
                        transactionDAO.recordStockIn(product.getId(), initialQty, product.getCostPrice(), "INIT-STOCK", "Initial stock when product was created", userId);
                    } catch (Exception ignored) {}
                }

                JsonUtil.sendSuccess(resp, "Product added successfully", product);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create product.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = JsonUtil.readRequestBody(req);
        Product product = JsonUtil.fromJson(body, Product.class);

        if (product == null || product.getId() <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid product ID is required for update.");
            return;
        }

        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product SKU is required.");
            return;
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product name is required.");
            return;
        }

        try {
            // Check SKU uniqueness excluding current product
            if (productDAO.existsBySku(product.getSku(), product.getId())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Another product with SKU '" + product.getSku() + "' already exists.");
                return;
            }

            boolean updated = productDAO.updateProduct(product);
            if (updated) {
                JsonUtil.sendSuccess(resp, "Product updated successfully", product);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found or not modified.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            // check body
            String body = JsonUtil.readRequestBody(req);
            Product p = JsonUtil.fromJson(body, Product.class);
            if (p != null && p.getId() > 0) {
                idParam = String.valueOf(p.getId());
            }
        }

        if (idParam == null || idParam.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID parameter is required.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());
            boolean deleted = productDAO.deleteProduct(id);
            if (deleted) {
                JsonUtil.sendSuccess(resp, "Product deleted successfully");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found with ID: " + id);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID format.");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Cannot delete product: " + e.getMessage());
        }
    }
}
