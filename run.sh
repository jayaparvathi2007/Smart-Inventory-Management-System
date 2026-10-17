#!/usr/bin/env bash
# ===============================================================
# SMART INVENTORY MANAGEMENT SYSTEM - RUN SCRIPT (LINUX/MAC)
# ===============================================================

set -e

if [ ! -f "bin/com/smartinventory/server/EmbeddedServer.class" ]; then
    echo "[*] Project not compiled yet. Running build.sh first..."
    chmod +x build.sh
    ./build.sh
fi

echo ""
echo "[*] Starting Smart Inventory Management System..."
echo "[*] Launching Embedded Server on port 8080..."
echo ""

java -cp "bin:lib/*" com.smartinventory.server.EmbeddedServer
