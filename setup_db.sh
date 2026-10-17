#!/usr/bin/env bash
# ===============================================================
# SMART INVENTORY MANAGEMENT SYSTEM - DATABASE SETUP (LINUX/MAC)
# ===============================================================

echo "==============================================================="
echo "    SMART INVENTORY MANAGEMENT SYSTEM - DATABASE SETUP"
echo "==============================================================="

DB_PASS=$1

if [ -z "$DB_PASS" ]; then
    read -s -p "Enter your MySQL root password (press Enter if blank): " DB_PASS
    echo ""
fi

echo "[*] Importing schema and seed data into MySQL..."

if [ -z "$DB_PASS" ]; then
    mysql -u root < database/schema.sql
else
    mysql -u root -p"$DB_PASS" < database/schema.sql
fi

if [ $? -ne 0 ]; then
    echo "[-] Failed to import database schema. Please check credentials."
    exit 1
fi

echo "[+] Database 'smart_inventory_db' created successfully!"

cat <<EOF > config/db.properties
# Smart Inventory Management System - Database Configuration
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/smart_inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=$DB_PASS
db.initialPoolSize=5
db.maxPoolSize=20
EOF

echo "[+] Updated config/db.properties with your credentials."
echo "[*] You can now compile and run with: ./run.sh"
