package com.smartinventory.dao;

import com.smartinventory.model.Category;
import com.smartinventory.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> getAllCategories() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT c.id, c.name, c.description, c.created_at, COUNT(p.id) AS product_count " +
                     "FROM categories c " +
                     "LEFT JOIN products p ON c.id = p.category_id " +
                     "GROUP BY c.id, c.name, c.description, c.created_at " +
                     "ORDER BY c.name ASC";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setProductCount(rs.getInt("product_count"));
                list.add(c);
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }

    public Category getCategoryById(int id) throws SQLException {
        String sql = "SELECT c.id, c.name, c.description, c.created_at, COUNT(p.id) AS product_count " +
                     "FROM categories c " +
                     "LEFT JOIN products p ON c.id = p.category_id " +
                     "WHERE c.id = ? " +
                     "GROUP BY c.id, c.name, c.description, c.created_at";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setProductCount(rs.getInt("product_count"));
                return c;
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return null;
    }

    public boolean addCategory(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, category.getName().trim());
            stmt.setString(2, category.getDescription());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
    }

    public boolean updateCategory(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, category.getName().trim());
            stmt.setString(2, category.getDescription());
            stmt.setInt(3, category.getId());

            return stmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(conn, stmt);
        }
    }

    public boolean deleteCategory(int id) throws SQLException {
        // Check if products exist in category first
        String checkSql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
        String deleteSql = "DELETE FROM categories WHERE id = ?";
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement deleteStmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, id);
            rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                throw new SQLException("Cannot delete category because it contains " + rs.getInt(1) + " product(s). Please reassign or delete the products first.");
            }

            deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setInt(1, id);
            return deleteStmt.executeUpdate() > 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (checkStmt != null) try { checkStmt.close(); } catch (SQLException ignored) {}
            if (deleteStmt != null) try { deleteStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    public boolean existsByName(String name, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?)";
        if (excludeId != null) {
            sql += " AND id <> ?";
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, name.trim());
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
}
