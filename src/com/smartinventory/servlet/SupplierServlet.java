package com.smartinventory.servlet;

import com.smartinventory.dao.SupplierDAO;
import com.smartinventory.model.Supplier;
import com.smartinventory.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/suppliers")
public class SupplierServlet extends HttpServlet {

    private final SupplierDAO supplierDAO = new SupplierDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                int id = Integer.parseInt(idParam.trim());
                Supplier supplier = supplierDAO.getSupplierById(id);
                if (supplier != null) {
                    JsonUtil.sendSuccess(resp, "Supplier found", supplier);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Supplier not found with ID: " + id);
                }
            } else {
                List<Supplier> list = supplierDAO.getAllSuppliers();
                JsonUtil.sendSuccess(resp, "Suppliers retrieved successfully", list);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid supplier ID format.");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = JsonUtil.readRequestBody(req);
        Supplier supplier = JsonUtil.fromJson(body, Supplier.class);

        if (supplier == null || supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Supplier name is required.");
            return;
        }

        try {
            if (supplierDAO.existsByName(supplier.getName(), null)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Supplier '" + supplier.getName() + "' already exists.");
                return;
            }

            boolean added = supplierDAO.addSupplier(supplier);
            if (added) {
                JsonUtil.sendSuccess(resp, "Supplier created successfully", supplier);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create supplier.");
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String body = JsonUtil.readRequestBody(req);
        Supplier supplier = JsonUtil.fromJson(body, Supplier.class);

        if (supplier == null || supplier.getId() <= 0 || supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Supplier ID and name are required.");
            return;
        }

        try {
            if (supplierDAO.existsByName(supplier.getName(), supplier.getId())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Another supplier with name '" + supplier.getName() + "' already exists.");
                return;
            }

            boolean updated = supplierDAO.updateSupplier(supplier);
            if (updated) {
                JsonUtil.sendSuccess(resp, "Supplier updated successfully", supplier);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Supplier not found or no changes made.");
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
            Supplier s = JsonUtil.fromJson(body, Supplier.class);
            if (s != null && s.getId() > 0) {
                idParam = String.valueOf(s.getId());
            }
        }

        if (idParam == null || idParam.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Supplier ID is required.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());
            boolean deleted = supplierDAO.deleteSupplier(id);
            if (deleted) {
                JsonUtil.sendSuccess(resp, "Supplier deleted successfully");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Supplier not found with ID: " + id);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid supplier ID format.");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }
}
