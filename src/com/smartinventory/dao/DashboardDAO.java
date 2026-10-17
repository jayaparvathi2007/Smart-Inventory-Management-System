package com.smartinventory.dao;

import com.smartinventory.model.DashboardSummary;
import com.smartinventory.model.DashboardSummary.CategoryStat;
import com.smartinventory.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    private final ProductDAO productDAO = new ProductDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public DashboardSummary getDashboardSummary() throws SQLException {
        DashboardSummary summary = new DashboardSummary();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();

            // 1. Products and stock aggregation
            String sqlStats = "SELECT " +
                    "COUNT(*) AS total_products, " +
                    "COALESCE(SUM(quantity), 0) AS total_units, " +
                    "COALESCE(SUM(quantity * cost_price), 0.00) AS total_cost_value, " +
                    "COALESCE(SUM(quantity * selling_price), 0.00) AS total_retail_value, " +
                    "COUNT(CASE WHEN quantity > 0 AND quantity <= min_stock_level THEN 1 END) AS low_stock_count, " +
                    "COUNT(CASE WHEN quantity <= 0 THEN 1 END) AS out_of_stock_count " +
                    "FROM products";
            stmt = conn.prepareStatement(sqlStats);
            rs = stmt.executeQuery();

            if (rs.next()) {
                summary.setTotalProducts(rs.getInt("total_products"));
                summary.setTotalStockUnits(rs.getInt("total_units"));
                summary.setTotalInventoryValue(rs.getBigDecimal("total_cost_value"));
                summary.setTotalRetailValue(rs.getBigDecimal("total_retail_value"));
                summary.setLowStockCount(rs.getInt("low_stock_count"));
                summary.setOutOfStockCount(rs.getInt("out_of_stock_count"));
            }
            rs.close();
            stmt.close();

            // 2. Count categories
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM categories");
            rs = stmt.executeQuery();
            if (rs.next()) {
                summary.setTotalCategories(rs.getInt(1));
            }
            rs.close();
            stmt.close();

            // 3. Count suppliers
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM suppliers");
            rs = stmt.executeQuery();
            if (rs.next()) {
                summary.setTotalSuppliers(rs.getInt(1));
            }
            rs.close();
            stmt.close();

            // 4. Category breakdown
            String sqlCatStats = "SELECT c.name, COUNT(p.id) AS prod_count, " +
                    "COALESCE(SUM(p.quantity), 0) AS total_stock, " +
                    "COALESCE(SUM(p.quantity * p.cost_price), 0.00) AS total_value " +
                    "FROM categories c " +
                    "LEFT JOIN products p ON c.id = p.category_id " +
                    "GROUP BY c.id, c.name " +
                    "ORDER BY total_stock DESC";
            stmt = conn.prepareStatement(sqlCatStats);
            rs = stmt.executeQuery();
            List<CategoryStat> catStats = new ArrayList<>();
            while (rs.next()) {
                catStats.add(new CategoryStat(
                        rs.getString("name"),
                        rs.getInt("prod_count"),
                        rs.getInt("total_stock"),
                        rs.getBigDecimal("total_value")
                ));
            }
            summary.setCategoryStats(catStats);

            // 5. Low stock alerts (top 10 items needing attention)
            summary.setLowStockProducts(productDAO.getLowStockProducts());

            // 6. Recent transactions (last 8)
            summary.setRecentTransactions(transactionDAO.getRecentTransactions(8));

        } finally {
            DBUtil.close(conn, stmt, rs);
        }

        return summary;
    }
}
