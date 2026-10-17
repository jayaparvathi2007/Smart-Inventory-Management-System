package com.smartinventory.servlet;

import com.smartinventory.dao.CategoryDAO;
import com.smartinventory.model.Category;
import com.smartinventory.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/categories")
public class CategoryServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                int id = Integer.parseInt(idParam.trim());
                Category category = categoryDAO.getCategoryById(id);
                if (category != null) {
                    JsonUtil.sendSuccess(resp, "Category found", category);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Category not found with ID: " + id);
                }
            } else {
                List<Category> list = categoryDAO.getAllCategories();
                JsonUtil.sendSuccess(resp, "Categories retrieved successfully", list);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid category ID format.");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = JsonUtil.readRequestBody(req);
        Category category = JsonUtil.fromJson(body, Category.class);

        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Category name is required.");
            return;
        }

        try {
            if (categoryDAO.existsByName(category.getName(), null)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Category '" + category.getName() + "' already exists.");
                return;
            }

            boolean added = categoryDAO.addCategory(category);
            if (added) {
                JsonUtil.sendSuccess(resp, "Category created successfully", category);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create category.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = JsonUtil.readRequestBody(req);
        Category category = JsonUtil.fromJson(body, Category.class);

        if (category == null || category.getId() <= 0 || category.getName() == null || category.getName().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Category ID and name are required.");
            return;
        }

        try {
            if (categoryDAO.existsByName(category.getName(), category.getId())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Another category with name '" + category.getName() + "' already exists.");
                return;
            }

            boolean updated = categoryDAO.updateCategory(category);
            if (updated) {
                JsonUtil.sendSuccess(resp, "Category updated successfully", category);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Category not found or no changes made.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            String body = JsonUtil.readRequestBody(req);
            Category c = JsonUtil.fromJson(body, Category.class);
            if (c != null && c.getId() > 0) {
                idParam = String.valueOf(c.getId());
            }
        }

        if (idParam == null || idParam.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Category ID is required.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());
            boolean deleted = categoryDAO.deleteCategory(id);
            if (deleted) {
                JsonUtil.sendSuccess(resp, "Category deleted successfully");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Category not found with ID: " + id);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid category ID format.");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }
}
