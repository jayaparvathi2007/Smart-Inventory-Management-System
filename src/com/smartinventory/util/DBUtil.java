package com.smartinventory.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database Utility class for managing JDBC connections and initializing schema.
 */
public class DBUtil {
    private static final Logger LOGGER = Logger.getLogger(DBUtil.class.getName());
    
    private static String jdbcUrl;
    private static String jdbcUsername;
    private static String jdbcPassword;
    private static String jdbcDriver;
    private static boolean initialized = false;

    static {
        loadConfiguration();
    }

    private static synchronized void loadConfiguration() {
        Properties props = new Properties();
        
        // 1. Try reading from config/db.properties in current directory
        File configFile = new File("config/db.properties");
        if (!configFile.exists()) {
            configFile = new File("../config/db.properties");
        }

        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
                LOGGER.info("Loaded database configuration from: " + configFile.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.warning("Could not read db.properties file: " + e.getMessage());
            }
        } else {
            // 2. Try loading from classpath
            try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                    LOGGER.info("Loaded db.properties from classpath");
                }
            } catch (Exception e) {
                LOGGER.warning("Could not read db.properties from classpath: " + e.getMessage());
            }
        }

        jdbcDriver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        jdbcUrl = System.getenv("DB_URL");
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            jdbcUrl = props.getProperty("db.url", 
                "jdbc:mysql://localhost:3306/smart_inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        }

        jdbcUsername = System.getenv("DB_USER");
        if (jdbcUsername == null || jdbcUsername.trim().isEmpty()) {
            jdbcUsername = props.getProperty("db.username", "root");
        }

        jdbcPassword = System.getenv("DB_PASSWORD");
        if (jdbcPassword == null) {
            jdbcPassword = props.getProperty("db.password", "root");
        }

        try {
            Class.forName(jdbcDriver);
            LOGGER.info("MySQL JDBC Driver registered successfully: " + jdbcDriver);
        } catch (ClassNotFoundException e) {
            LOGGER.severe("MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }

        initialized = true;
    }

    /**
     * Get a new database connection.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }
        try {
            return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
        } catch (SQLException e) {
            // Reload configuration in case db.properties was updated while running
            loadConfiguration();
            return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
        }
    }

    /**
     * Test connection and return status message.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Close standard JDBC resources quietly.
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException ignored) {}
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }

    public static void close(Statement stmt) {
        close(null, stmt, null);
    }

    public static void close(Connection conn) {
        close(conn, null, null);
    }

    /**
     * Updates runtime database credentials (e.g. if configured from prompt).
     */
    public static synchronized void setCredentials(String url, String username, String password) {
        if (url != null) jdbcUrl = url;
        if (username != null) jdbcUsername = username;
        if (password != null) jdbcPassword = password;
    }

    public static String getJdbcUrl() { return jdbcUrl; }
    public static String getJdbcUsername() { return jdbcUsername; }
}
