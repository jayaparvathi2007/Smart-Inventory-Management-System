package com.smartinventory.dao;

import com.smartinventory.model.Product;
import com.smartinventory.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        
        int suppId = rs.getInt("supplier_id");
        if (!rs.wasNull()) {
            p.setSupplierId(suppId);
            p.setSupplierName(rs.getString("supplier_name"));
        } else {
            p.setSupplierId(null);
            p.setSupplierName("N/A");
        }

        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setQuantity(rs.getInt("quantity"));
        p.setMinStockLevel(rs.getInt("min_stock_level"));
        p.setDescription(rs.getString("description"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }

    private String getBaseSelectQuery() {
        return "SELECT p.id, p.sku, p.name, p.category_id, c.name AS category_name, " +
               "p.supplier_id, s.name AS supplier_name, p.cost_price, p.selling_price, " +
               "p.quantity, p.min_stock_level, p.description, p.created_at, p.updated_at " +
               "FROM products p " +
               "JOIN categories c ON p.category_id = c.id " +
               "LEFT JOIN suppliers s ON p.supplier_id = s.id ";
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " ORDER BY p.name ASC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }

    public Product getProductById(int id) throws SQLException {
        String sql = getBaseSelectQuery() + " WHERE p.id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return null;
    }

    public Product getProductBySku(String sku) throws SQLException {
        String sql = getBaseSelectQuery() + " WHERE p.sku = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, sku);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return null;
    }

    public List<Product> searchProducts(String query, Integer categoryId, String stockStatus) throws SQLException {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(getBaseSelectQuery());
        sql.append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(p.name) LIKE ? OR LOWER(p.sku) LIKE ? OR LOWER(p.description) LIKE ?) ");
            String term = "%" + query.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND p.category_id = ? ");
            params.add(categoryId);
        }

        if (stockStatus != null) {
            switch (stockStatus.toUpperCase()) {
                case "OUT_OF_STOCK":
                    sql.append(" AND p.quantity <= 0 ");
                    break;
                case "LOW_STOCK":
                    sql.append(" AND p.quantity > 0 AND p.quantity <= p.min_stock_level ");
                    break;
                case "IN_STOCK":
                    sql.append(" AND p.quantity > p.min_stock_level ");
                    break;
            }
        }

        sql.append(" ORDER BY p.name ASC");

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

    public List<Product> getLowStockProducts() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = getBaseSelectQuery() + " WHERE p.quantity <= p.min_stock_level ORDER BY p.quantity ASC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }

    public boolean addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products (sku, name, category_id, supplier_id, cost_price, selling_price, quantity, min_stock_level, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, product.getSku().trim());
            stmt.setString(2, product.getName().trim());
            stmt.setInt(3, product.getCategoryId());
            if (product.getSupplierId() != null && product.getSupplierId() > 0) {
                stmt.setInt(4, product.getSupplierId());
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setBigDecimal(5, product.getCostPrice());
            stmt.setBigDecimal(6, product.getSellingPrice());
            stmt.setInt(7, product.getQuantity());
            stmt.setInt(8, product.getMinStockLevel());
            stmt.setString(9, product.getDescription());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    product.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
    }

    public boolean updateProduct(Product product) throws SQLException {
        String sql = "UPDATE products SET sku = ?, name = ?, category_id = ?, supplier_id = ?, " +
                     "cost_price = ?, selling_price = ?, min_stock_level = ?, description = ? " +
                     "WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, product.getSku().trim());
            stmt.setString(2, product.getName().trim());
            stmt.setInt(3, product.getCategoryId());
            if (product.getSupplierId() != null && product.getSupplierId() > 0) {
                stmt.setInt(4, product.getSupplierId());
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setBigDecimal(5, product.getCostPrice());
            stmt.setBigDecimal(6, product.getSellingPrice());
            stmt.setInt(7, product.getMinStockLevel());
            stmt.setString(8, product.getDescription());
            stmt.setInt(9, product.getId());

            return stmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(conn, stmt);
        }
    }

    public boolean deleteProduct(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(conn, stmt);
        }
    }

    public boolean existsBySku(String sku, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE LOWER(sku) = LOWER(?)";
        if (excludeId != null) {
            sql += " AND id <> ?";
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, sku.trim());
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
    }

    /**
     * Updates product stock with delta quantity (positive for stock-in, negative for stock-out).
     * Must be called within an active transaction.
     */
    public boolean updateStockDelta(Connection conn, int productId, int delta) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ? AND (quantity + ?) >= 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, productId);
            stmt.setInt(3, delta);
            int updated = stmt.executeUpdate();
            return updated > 0;
        }
    }
}
