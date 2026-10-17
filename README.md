# 📦 Smart Inventory Management System

A full-stack, enterprise-grade, beginner-friendly **Smart Inventory Management System** built with **HTML5, CSS3, JavaScript, Core Java, Java Servlets, JDBC, and MySQL**.

Designed for real-world inventory control, stock-in/stock-out operations, supplier tracking, category classification, low-stock threshold alerting, and printable/downloadable valuation and audit reporting.

---

## 🌟 Key Features

### 1. 📊 Real-Time Analytics Dashboard
- **KPI Summary Cards**: Live counts of total unique products, total inventory units, total stock valuation (cost basis), and real-time low-stock/out-of-stock warning counts.
- **Low-Stock Alert Center**: Automatically detects products at or below their minimum reorder point. Includes a **1-click "+ Stock In" button** for instant restocking.
- **Stock by Category Breakdown**: Visual progress indicators showing category stock distribution and percentage shares.
- **Recent Activity Feed**: Live audit stream of the most recent stock additions and sales dispatches.

### 2. 🏷️ Complete Product Management (CRUD + Search)
- **Add, View, Edit, and Delete** products with instant server-side & client-side validation.
- Fields: SKU code (unique), Name, Category, Supplier, Cost Price, Selling Price, Initial Stock Quantity, Minimum Stock Alert Level, and Description.
- **Multi-Filter & Instant Search**: Search by name or SKU, filter by category, or filter by stock health (*All, In Stock, Low Stock, Out of Stock*).
- Safe deletion verification with foreign key relationship checks.

### 3. 📂 Categories & 🚚 Suppliers Management
- **Category Control**: Organize products into organized categories (e.g., Electronics, Office Supplies, Furniture, Hardware). Displays live counts of products tied to each category.
- **Supplier Directory**: Track company names, contact persons, email addresses, phone numbers, and physical warehouse locations. Shows total products sourced from each supplier.

### 4. 🔄 Stock-In & Stock-Out with Transaction History
- **Stock-In (Receive Inventory)**: Add incoming stock batches with supplier PO/Invoice references, purchase prices, and batch notes. Automatically updates product quantity.
- **Stock-Out (Sales & Dispatch)**: Deduct outgoing shipments or sales orders. **Includes live stock protection** to prevent negative inventory if requested quantity exceeds available stock.
- **Audit Log & Movement History**: Comprehensive log storing Date, Product, Type (`STOCK_IN` / `STOCK_OUT`), Quantity, Unit Price, Total Value, Reference #, and the logged-in User who authorized the movement. Filterable by date range and movement type.

### 5. 📈 Inventory Reports & CSV Exports
- **Inventory Valuation Report**: Summarizes total items, cost investments, prospective retail value, and gross profit margin.
- **Low-Stock & Reorder Report**: Pinpoints items needing reorder, calculates unit deficits to reach safe stock levels, and estimates reorder investment costs.
- **Transaction Movements Audit Report**: Complete chronological movement history.
- **Export & Print**: Download any report as a `.csv` spreadsheet or use the **"Print Sheet"** button for a clean print/PDF layout.

### 6. 🔐 Secure Authentication & Role Management
- Simple, modern login interface.
- Session-based authentication (`HttpSession`) with `AuthFilter` route protection.
- Pre-seeded with **Admin** (`admin` / `admin123`) and **Manager** (`manager` / `manager123`) accounts.

---

## 🏗️ Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Frontend** | HTML5, CSS3, JavaScript (ES6) | Responsive, mobile-friendly UI, CSS Grid & Flexbox, Toast notifications, Modal dialogs, Async Fetch API |
| **Backend** | Core Java, Java Servlets (Java EE 7 / Jakarta EE 8/9 compatible) | Pure JDBC, MVC architecture, DAO design pattern, thread-safe session handling |
| **Database** | MySQL (5.7+ / 8.0+) | InnoDB engine, normalized tables, foreign key constraints, indexes, atomic transactions |
| **Libraries** | `mysql-connector-java-8.0.28.jar`<br>`javax.servlet-api-4.0.1.jar`<br>`gson-2.10.1.jar` | Included in `lib/` and `webapp/WEB-INF/lib/` |

---

## 📁 Project Structure

```
smart-inventory-system/
├── bin/                             # Compiled Java bytecode (.class files)
├── config/
│   └── db.properties                # Database connection settings (URL, user, password)
├── database/
│   └── schema.sql                   # MySQL database creation script with sample seed data
├── lib/                             # JAR dependencies
│   ├── gson-2.10.1.jar
│   ├── javax.servlet-api-4.0.1.jar
│   └── mysql-connector-java-8.0.28.jar
├── src/
│   └── com/smartinventory/
│       ├── dao/                     # Data Access Objects (JDBC queries & transactions)
│       │   ├── CategoryDAO.java
│       │   ├── DashboardDAO.java
│       │   ├── ProductDAO.java
│       │   ├── SupplierDAO.java
│       │   ├── TransactionDAO.java
│       │   └── UserDAO.java
│       ├── filter/
│       │   └── AuthFilter.java      # Session authentication & route protection filter
│       ├── model/                   # Data Models / POJOs
│       │   ├── Category.java
│       │   ├── DashboardSummary.java
│       │   ├── InventoryTransaction.java
│       │   ├── Product.java
│       │   ├── Supplier.java
│       │   └── User.java
│       ├── server/
│       │   └── EmbeddedServer.java  # Built-in lightweight runner (Runs without external Tomcat!)
│       ├── servlet/                 # Standard Java HTTP Servlets
│       │   ├── AuthServlet.java
│       │   ├── CategoryServlet.java
│       │   ├── DashboardServlet.java
│       │   ├── ProductServlet.java
│       │   ├── ReportServlet.java
│       │   ├── StockServlet.java
│       │   └── SupplierServlet.java
│       └── util/                    # Helper utilities
│           ├── DatabaseInitializer.java
│           ├── DBUtil.java
│           ├── JsonUtil.java
│           └── PasswordUtil.java
├── webapp/                          # Web assets (Frontend & WAR root)
│   ├── css/
│   │   └── style.css                # Modern responsive stylesheet
│   ├── js/
│   │   ├── app.js                   # Session, toasts, modal controllers
│   │   ├── categories.js            # Category CRUD logic
│   │   ├── dashboard.js             # Dashboard charts & KPI counters
│   │   ├── products.js              # Product CRUD, filters & modals
│   │   ├── reports.js               # Report generation & exports
│   │   ├── stock.js                 # Stock in/out logic & movements table
│   │   └── suppliers.js             # Supplier directory logic
│   ├── WEB-INF/
│   │   ├── classes/                 # Compiled classes for Tomcat deployment
│   │   ├── lib/                     # Libraries for Tomcat deployment
│   │   └── web.xml                  # Standard Java EE web deployment descriptor
│   ├── categories.html              # Category management page
│   ├── index.html                   # Main dashboard view
│   ├── login.html                   # Authentication view
│   ├── products.html                # Product catalogue view
│   ├── reports.html                 # Inventory analytics view
│   ├── stock.html                   # Stock in/out movements view
│   └── suppliers.html               # Supplier directory view
├── build.bat                        # Windows 1-click build script
├── run.bat                          # Windows 1-click launch script
├── setup_db.bat                     # Windows database setup script
├── build.sh                         # Linux/macOS build script
├── run.sh                           # Linux/macOS run script
├── setup_db.sh                      # Linux/macOS database setup script
└── README.md                        # Documentation & setup guide
```

---

## 🚀 Getting Started

### Prerequisites
1. **Java Development Kit (JDK 8 or higher)**: Run `java -version` and `javac -version` in terminal to verify. (Tested and compatible with JDK 8, 11, 17, 21, and 27+).
2. **MySQL Server (5.7 or 8.0+)**: Running locally on port `3306`.

---

### Step 1: Configure the Database

#### Method A: Using the Automated Setup Script (Recommended)
Open a terminal in the project directory and run:

**On Windows:**
```cmd
setup_db.bat
```
*(Enter your MySQL root password when prompted; press Enter if there is no password)*

**On Linux / macOS:**
```bash
chmod +x *.sh
./setup_db.sh
```

#### Method B: Manual MySQL Import
If you prefer running the SQL script manually through MySQL Command Line or MySQL Workbench:
```cmd
mysql -u root -p < database/schema.sql
```
Then open `config/db.properties` and verify your password:
```properties
db.url=jdbc:mysql://localhost:3306/smart_inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=YOUR_MYSQL_PASSWORD
```

---

### Step 2: Build and Run the System

You can run the system in two ways:

#### Option 1: Standalone Runner (No external Tomcat installation required!)
We have included a built-in server launcher (`EmbeddedServer.java`) so you can test and run the project immediately with zero configuration:

**On Windows:**
Double-click `run.bat` or run in terminal:
```cmd
run.bat
```

**On Linux / macOS:**
```bash
./run.sh
```

Open your browser and navigate to:
👉 **[http://localhost:8080/](http://localhost:8080/)**

#### Option 2: Run in Apache Tomcat / Eclipse / IntelliJ IDEA
Because this project strictly follows the Java EE Servlet specification:
- **Apache Tomcat**: Copy the contents of `webapp/` into Tomcat's `webapps/smart-inventory/` folder, or create a WAR archive using `jar -cvf smart-inventory.war -C webapp/ .` and place it in `webapps/`.
- **Eclipse**: File -> Import -> Existing Projects into Workspace, or create a "Dynamic Web Project", set Web content folder to `webapp`, source folder to `src`, and add the JARs from `lib/` to the Build Path.
- **IntelliJ IDEA**: Open project folder, configure Project SDK to Java, add `lib/` as a Project Library, configure a local Tomcat Server Run Configuration pointing to `webapp/`.

---

## 🔑 Default Login Credentials

| Username | Password | Role | Permissions |
|---|---|---|---|
| `admin` | `admin123` | Administrator | Full access to all modules, transactions, and reports |
| `manager` | `manager123` | Manager | Stock movements, products, suppliers, and reports |

*(You can also click the quick demo credentials button on the login screen to auto-fill them!)*

---

## 🔌 API Endpoints Reference

All API routes communicate using clean JSON and standard HTTP methods:

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth` | User login (Accepts JSON `{username, password}`) |
| `GET` | `/api/auth` | Checks active session information |
| `POST` | `/api/auth?action=logout` | Terminates active session |
| `GET` | `/api/dashboard` | Returns aggregated metrics, low stock alerts, and category distributions |
| `GET` | `/api/products` | Lists products with optional query, category, and status filters |
| `GET` | `/api/products?id={id}` | Retrieves a single product by ID |
| `POST` | `/api/products` | Creates a new product and logs initial stock |
| `PUT` | `/api/products` | Updates an existing product |
| `DELETE` | `/api/products?id={id}` | Deletes a product |
| `GET` | `/api/categories` | Lists all product categories with product counts |
| `POST` | `/api/categories` | Creates a new category |
| `PUT` | `/api/categories` | Updates an existing category |
| `DELETE` | `/api/categories?id={id}` | Deletes a category (protects categories with products) |
| `GET` | `/api/suppliers` | Lists all suppliers with product counts |
| `POST` | `/api/suppliers` | Creates a new supplier |
| `PUT` | `/api/suppliers` | Updates an existing supplier |
| `DELETE` | `/api/suppliers?id={id}` | Deletes a supplier |
| `POST` | `/api/stock` | Processes atomic Stock In or Stock Out (`{action: 'in'/'out', productId, quantity, ...}`) |
| `GET` | `/api/stock` | Retrieves transaction movements history with date filters |
| `GET` | `/api/reports?type=valuation` | Generates inventory valuation report |
| `GET` | `/api/reports?type=lowstock` | Generates low stock reorder schedule |
| `GET` | `/api/reports?type=transactions` | Generates transaction movements audit report |
| `GET` | `/api/reports?type=...&export=csv` | Directly streams and downloads report as a CSV file |

---

## 🛡️ Database Security & Robustness
- **Prepared Statements**: Used for every single database query to prevent SQL Injection attacks.
- **Atomic Transactions**: Stock additions and dispatches use `conn.setAutoCommit(false)`, updating product quantity and recording the audit log within a single transaction boundary with `commit()` and `rollback()`.
- **Negative Stock Prevention**: Stock-Out operations verify inventory availability with `SELECT ... FOR UPDATE` row-level locking to prevent race conditions and overdrafts.
- **Password Protection**: Supports SHA-256 password hashing.
- **Input Validation**: Both client-side HTML5/JS validation and server-side Java null/boundary checks are implemented for all inputs.

---

## 📝 License
This project is open-source and free to use for educational, personal, and commercial purposes.
