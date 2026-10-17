package com.smartinventory.servlet;

import com.smartinventory.dao.ProductDAO;
import com.smartinventory.dao.TransactionDAO;
import com.smartinventory.model.InventoryTransaction;
import com.smartinventory.model.Product;
import com.smartinventory.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/reports")
public class ReportServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        String export = req.getParameter("export");

        if (type == null || type.trim().isEmpty()) {
            type = "valuation";
        }

        try {
            if ("csv".equalsIgnoreCase(export)) {
                handleCsvExport(req, resp, type);
            } else {
                handleJsonReport(req, resp, type);
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error generating report: " + e.getMessage());
        }
    }

    private void handleJsonReport(HttpServletRequest req, HttpServletResponse resp, String type) throws SQLException, IOException {
        Map<String, Object> report = new HashMap<>();
        report.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        report.put("reportType", type);

        if ("valuation".equalsIgnoreCase(type) || "inventory".equalsIgnoreCase(type)) {
            List<Product> products = productDAO.getAllProducts();
            BigDecimal totalCostVal = BigDecimal.ZERO;
            BigDecimal totalRetailVal = BigDecimal.ZERO;
            int totalUnits = 0;

            for (Product p : products) {
                totalUnits += p.getQuantity();
                totalCostVal = totalCostVal.add(p.getTotalCostValue());
                totalRetailVal = totalRetailVal.add(p.getTotalRetailValue());
            }

            report.put("products", products);
            report.put("totalProducts", products.size());
            report.put("totalUnits", totalUnits);
            report.put("totalCostValuation", totalCostVal);
            report.put("totalRetailValuation", totalRetailVal);
            report.put("estimatedMargin", totalRetailVal.subtract(totalCostVal));

            JsonUtil.sendSuccess(resp, "Inventory Valuation Report generated", report);
        } else if ("lowstock".equalsIgnoreCase(type)) {
            List<Product> products = productDAO.getLowStockProducts();
            report.put("products", products);
            report.put("lowStockCount", products.size());
            JsonUtil.sendSuccess(resp, "Low Stock Reorder Report generated", report);
        } else if ("transactions".equalsIgnoreCase(type)) {
            String startDate = req.getParameter("startDate");
            String endDate = req.getParameter("endDate");
            String transType = req.getParameter("transType");

            List<InventoryTransaction> transactions = transactionDAO.getTransactions(null, transType, startDate, endDate, 500);
            report.put("transactions", transactions);
            report.put("totalCount", transactions.size());
            JsonUtil.sendSuccess(resp, "Transaction History Report generated", report);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown report type: " + type);
        }
    }

    private void handleCsvExport(HttpServletRequest req, HttpServletResponse resp, String type) throws SQLException, IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "inventory_report_" + type + "_" + timestamp + ".csv";

        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PrintWriter out = resp.getWriter();

        if ("lowstock".equalsIgnoreCase(type)) {
            List<Product> list = productDAO.getLowStockProducts();
            out.println("SKU,Product Name,Category,Supplier,Quantity,Min Stock Level,Deficit,Cost Price,Status");
            for (Product p : list) {
                int deficit = Math.max(0, p.getMinStockLevel() - p.getQuantity());
                out.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%d,%.2f,\"%s\"%n",
                        escapeCsv(p.getSku()),
                        escapeCsv(p.getName()),
                        escapeCsv(p.getCategoryName()),
                        escapeCsv(p.getSupplierName()),
                        p.getQuantity(),
                        p.getMinStockLevel(),
                        deficit,
                        p.getCostPrice(),
                        escapeCsv(p.getStockStatus()));
            }
        } else if ("transactions".equalsIgnoreCase(type)) {
            String startDate = req.getParameter("startDate");
            String endDate = req.getParameter("endDate");
            String transType = req.getParameter("transType");
            List<InventoryTransaction> list = transactionDAO.getTransactions(null, transType, startDate, endDate, 1000);

            out.println("ID,Date,Product SKU,Product Name,Type,Quantity,Unit Price,Total,Reference,Notes,User");
            for (InventoryTransaction t : list) {
                out.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,\"%s\",\"%s\",\"%s\"%n",
                        t.getId(),
                        t.getTransactionDate(),
                        escapeCsv(t.getProductSku()),
                        escapeCsv(t.getProductName()),
                        escapeCsv(t.getTransactionType()),
                        t.getQuantity(),
                        t.getUnitPrice(),
                        t.getTotalAmount(),
                        escapeCsv(t.getReferenceNo() != null ? t.getReferenceNo() : ""),
                        escapeCsv(t.getNotes() != null ? t.getNotes() : ""),
                        escapeCsv(t.getUserName() != null ? t.getUserName() : ""));
            }
        } else {
            // Valuation report
            List<Product> list = productDAO.getAllProducts();
            out.println("SKU,Product Name,Category,Supplier,Quantity,Cost Price,Selling Price,Total Cost Value,Total Retail Value,Status");
            for (Product p : list) {
                out.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,%.2f,%.2f,\"%s\"%n",
                        escapeCsv(p.getSku()),
                        escapeCsv(p.getName()),
                        escapeCsv(p.getCategoryName()),
                        escapeCsv(p.getSupplierName()),
                        p.getQuantity(),
                        p.getCostPrice(),
                        p.getSellingPrice(),
                        p.getTotalCostValue(),
                        p.getTotalRetailValue(),
                        escapeCsv(p.getStockStatus()));
            }
        }

        out.flush();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
