package com.smartinventory.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Pure Java Database Initializer.
 * Connects via JDBC to create 'smart_inventory_db' and execute 'database/schema.sql'.
 */
public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  SMART INVENTORY MANAGEMENT SYSTEM - PURE JAVA DB INITIALIZER    ");
        System.out.println("==================================================================");

        Scanner scanner = new Scanner(System.in);
        String host = "localhost";
        int port = 3306;
        String user = "root";
        String password = "";

        if (args.length > 0) {
            password = args[0];
        } else {
            System.out.print("Enter MySQL root password (press Enter if blank or 'root'): ");
            password = scanner.nextLine().trim();
            if (password.isEmpty()) {
                password = "root"; // default attempt
            }
        }

        String serverUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&allowMultiQueries=true";

        System.out.println("[*] Connecting to MySQL server at " + host + ":" + port + " as user '" + user + "'...");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[-] Error: MySQL JDBC driver not found in classpath.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(serverUrl, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("[+] Connected to MySQL Server!");

            File schemaFile = new File("database/schema.sql");
            if (!schemaFile.exists()) {
                schemaFile = new File("../database/schema.sql");
            }

            if (!schemaFile.exists()) {
                System.err.println("[-] Error: database/schema.sql file not found.");
                return;
            }

            System.out.println("[*] Reading SQL schema from: " + schemaFile.getAbsolutePath());
            StringBuilder sqlBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(schemaFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("--") || line.startsWith("//") || line.isEmpty()) {
                        continue;
                    }
                    sqlBuilder.append(line).append("\n");
                }
            }

            String fullSql = sqlBuilder.toString();
            String[] statements = fullSql.split(";");

            System.out.println("[*] Executing database creation & schema statements (" + statements.length + " statements)...");

            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception ex) {
                        System.err.println("[-] Warning on statement: " + trimmed.substring(0, Math.min(60, trimmed.length())) + "... -> " + ex.getMessage());
                    }
                }
            }

            System.out.println("[+] Database 'smart_inventory_db' initialized and seeded successfully!");

            // Update DBUtil credentials
            DBUtil.setCredentials("jdbc:mysql://localhost:3306/smart_inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8", user, password);

        } catch (Exception e) {
            System.err.println("[-] Failed to initialize database: " + e.getMessage());
            System.err.println("[-] Please check your password or make sure MySQL service is running.");
        }
    }
}
