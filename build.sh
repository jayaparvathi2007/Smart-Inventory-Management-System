#!/usr/bin/env bash
# ===============================================================
# SMART INVENTORY MANAGEMENT SYSTEM - BUILD SCRIPT (LINUX/MAC)
# ===============================================================

set -e

echo "[*] Creating bin directory..."
mkdir -p bin
mkdir -p webapp/WEB-INF/classes

echo "[*] Compiling Java source files..."
javac -encoding UTF-8 -cp "lib/*" -d bin src/com/smartinventory/model/*.java src/com/smartinventory/dao/*.java src/com/smartinventory/util/*.java src/com/smartinventory/filter/*.java src/com/smartinventory/servlet/*.java src/com/smartinventory/server/*.java

echo "[*] Copying compiled classes to webapp/WEB-INF/classes..."
cp -r bin/* webapp/WEB-INF/classes/

echo "[+] BUILD SUCCESSFUL! All classes compiled cleanly."
echo "[*] To start the server, execute: ./run.sh"
