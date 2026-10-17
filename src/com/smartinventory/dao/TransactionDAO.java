package com.smartinventory.dao;

import com.smartinventory.model.InventoryTransaction;
import com.smartinventory.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    private InventoryTransaction mapRow(ResultSet rs) throws SQLException {
        InventoryTransaction t = new InventoryTransaction();
        t.setId(rs.getInt("id"));
        t.setProductId(rs.getInt("product_id"));
        t.setProductSku(rs.getString("product_sku"));
        t.setProductName(rs.getString("product_name"));
        t.setTransactionType(rs.getString("transaction_type"));
        t.setQuantity(rs.getInt("quantity"));
        t.setUnitPrice(rs.getBigDecimal("unit_price"));
        t.setReferenceNo(rs.getString("reference_no"));
        t.setNotes(rs.getString("notes"));
        
        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) {
            t.setUserId(uid);
            t.setUserName(rs.getString("user_name"));
        } else {
            t.setUserId(null);
            t.setUserName("System");
        }

        t.setTransactionDate(rs.getTimestamp("transaction_date"));
        return t;
    }

    private String getBaseSelectQuery() {
        return "SELECT t.id, t.product_id, p.sku AS product_sku, p.name AS product_name, " +
               "t.transaction_type, t.quantity, t.unit_price, t.reference_no, t.notes, " +
               "t.user_id, u.full_name AS user_name, t.transaction_date " +
               "FROM inventory_transactions t " +
               "JOIN products p ON t.product_id = p.id " +
               "LEFT JOIN users u ON t.user_id = u.id ";
    }

    /**
     * Executes atomic Stock In: updates product quantity and logs transaction.
     */
    public boolean recordStockIn(int productId, int quantity, BigDecimal unitPrice, String referenceNo, String notes, Integer userId) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Connection conn = null;
        PreparedStatement updateStmt = null;
        PreparedStatement insertStmt = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Update product quantity
            String updateSql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, quantity);
            updateStmt.setInt(2, productId);
            int rowsUpdated = updateStmt.executeUpdate();

            if (rowsUpdated == 0) {
                conn.rollback();
                throw new SQLException("Product with ID " + productId + " was not found.");
            }

            // 2. Insert transaction record
            String insertSql = "INSERT INTO inventory_transactions (product_id, transaction_type, quantity, unit_price, reference_no, notes, user_id) " +
                               "VALUES (?, 'STOCK_IN', ?, ?, ?, ?, ?)";
            insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, productId);
            insertStmt.setInt(2, quantity);
            insertStmt.setBigDecimal(3, unitPrice != null ? unitPrice : BigDecimal.ZERO);
            insertStmt.setString(4, referenceNo);
            insertStmt.setString(5, notes);
            if (userId != null && userId > 0) {
                insertStmt.setInt(6, userId);
            } else {
                insertStmt.setNull(6, java.sql.Types.INTEGER);
            }
            insertStmt.executeUpdate();

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (updateStmt != null) try { updateStmt.close(); } catch (SQLException ignored) {}
            if (insertStmt != null) try { insertStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Executes atomic Stock Out: verifies sufficient quantity, decrements stock, and logs transaction.
     */
    public boolean recordStockOut(int productId, int quantity, BigDecimal unitPrice, String referenceNo, String notes, Integer userId) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement updateStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Check current stock level with FOR UPDATE to prevent race conditions
            String checkSql = "SELECT quantity, name FROM products WHERE id = ? FOR UPDATE";
            checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, productId);
            rs = checkStmt.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                throw new SQLException("Product with ID " + productId + " was not found.");
            }

            int currentStock = rs.getInt("quantity");
            String productName = rs.getString("name");

            if (currentStock < quantity) {
                conn.rollback();
                throw new SQLException("Insufficient stock for '" + productName + "'. Available: " + currentStock + ", Requested: " + quantity);
            }

            // 2. Decrement product quantity
            String updateSql = "UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, quantity);
            updateStmt.setInt(2, productId);
            updateStmt.setInt(3, quantity);
            int rowsUpdated = updateStmt.executeUpdate();

            if (rowsUpdated == 0) {
                conn.rollback();
                throw new SQLException("Failed to update stock. Stock level may have changed concurrently.");
            }

            // 3. Insert transaction record
            String insertSql = "INSERT INTO inventory_transactions (product_id, transaction_type, quantity, unit_price, reference_no, notes, user_id) " +
                               "VALUES (?, 'STOCK_OUT', ?, ?, ?, ?, ?)";
            insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, productId);
            insertStmt.setInt(2, quantity);
            insertStmt.setBigDecimal(3, unitPrice != null ? unitPrice : BigDecimal.ZERO);
            insertStmt.setString(4, referenceNo);
            insertStmt.setString(5, notes);
            if (userId != null && userId > 0) {
                insertStmt.setInt(6, userId);
            } else {
                insertStmt.setNull(6, java.sql.Types.INTEGER);
            }
            insertStmt.executeUpdate();

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (checkStmt != null) try { checkStmt.close(); } catch (SQLException ignored) {}
            if (updateStmt != null) try { updateStmt.close(); } catch (SQLException ignored) {}
            if (insertStmt != null) try { insertStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<InventoryTransaction> getRecentTransactions(int limit) throws SQLException {
        List<InventoryTransaction> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " ORDER BY t.transaction_date DESC, t.id DESC LIMIT ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit > 0 ? limit : 10);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }

    public List<InventoryTransaction> getTransactions(Integer productId, String type, String startDate, String endDate, int limit) throws SQLException {
        List<InventoryTransaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(getBaseSelectQuery());
        sql.append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (productId != null && productId > 0) {
            sql.append(" AND t.product_id = ? ");
            params.add(productId);
        }

        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type.trim())) {
            sql.append(" AND t.transaction_type = ? ");
            params.add(type.trim().toUpperCase());
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND DATE(t.transaction_date) >= ? ");
            params.add(startDate.trim());
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND DATE(t.transaction_date) <= ? ");
            params.add(endDate.trim());
        }

        sql.append(" ORDER BY t.transaction_date DESC, t.id DESC ");

        if (limit > 0) {
            sql.append(" LIMIT ? ");
            params.add(limit);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }
}
