package com.smartinventory.dao;

import com.smartinventory.model.Supplier;
import com.smartinventory.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    public List<Supplier> getAllSuppliers() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT s.id, s.name, s.contact_person, s.email, s.phone, s.address, s.created_at, COUNT(p.id) AS product_count " +
                     "FROM suppliers s " +
                     "LEFT JOIN products p ON s.id = p.supplier_id " +
                     "GROUP BY s.id, s.name, s.contact_person, s.email, s.phone, s.address, s.created_at " +
                     "ORDER BY s.name ASC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Supplier s = new Supplier();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setContactPerson(rs.getString("contact_person"));
                s.setEmail(rs.getString("email"));
                s.setPhone(rs.getString("phone"));
                s.setAddress(rs.getString("address"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                s.setProductCount(rs.getInt("product_count"));
                list.add(s);
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }

    public Supplier getSupplierById(int id) throws SQLException {
        String sql = "SELECT s.id, s.name, s.contact_person, s.email, s.phone, s.address, s.created_at, COUNT(p.id) AS product_count " +
                     "FROM suppliers s " +
                     "LEFT JOIN products p ON s.id = p.supplier_id " +
                     "WHERE s.id = ? " +
                     "GROUP BY s.id, s.name, s.contact_person, s.email, s.phone, s.address, s.created_at";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Supplier s = new Supplier();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setContactPerson(rs.getString("contact_person"));
                s.setEmail(rs.getString("email"));
                s.setPhone(rs.getString("phone"));
                s.setAddress(rs.getString("address"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                s.setProductCount(rs.getInt("product_count"));
                return s;
            }
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return null;
    }

    public boolean addSupplier(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO suppliers (name, contact_person, email, phone, address) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, supplier.getName().trim());
            stmt.setString(2, supplier.getContactPerson());
            stmt.setString(3, supplier.getEmail());
            stmt.setString(4, supplier.getPhone());
            stmt.setString(5, supplier.getAddress());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    supplier.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
    }

    public boolean updateSupplier(Supplier supplier) throws SQLException {
        String sql = "UPDATE suppliers SET name = ?, contact_person = ?, email = ?, phone = ?, address = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, supplier.getName().trim());
            stmt.setString(2, supplier.getContactPerson());
            stmt.setString(3, supplier.getEmail());
            stmt.setString(4, supplier.getPhone());
            stmt.setString(5, supplier.getAddress());
            stmt.setInt(6, supplier.getId());

            return stmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(conn, stmt);
        }
    }

    public boolean deleteSupplier(int id) throws SQLException {
        // Disassociate products from this supplier or check
        String sql = "DELETE FROM suppliers WHERE id = ?";
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

    public boolean existsByName(String name, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM suppliers WHERE LOWER(name) = LOWER(?)";
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
