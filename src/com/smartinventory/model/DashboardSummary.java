package com.smartinventory.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardSummary {
    private int totalProducts;
    private int totalStockUnits;
    private BigDecimal totalInventoryValue;
    private BigDecimal totalRetailValue;
    private int lowStockCount;
    private int outOfStockCount;
    private int totalCategories;
    private int totalSuppliers;
    private List<Product> lowStockProducts;
    private List<InventoryTransaction> recentTransactions;
    private List<CategoryStat> categoryStats;

    public DashboardSummary() {
        this.totalInventoryValue = BigDecimal.ZERO;
        this.totalRetailValue = BigDecimal.ZERO;
        this.lowStockProducts = new ArrayList<>();
        this.recentTransactions = new ArrayList<>();
        this.categoryStats = new ArrayList<>();
    }

    public static class CategoryStat {
        private String categoryName;
        private int productCount;
        private int totalStock;
        private BigDecimal totalValue;

        public CategoryStat(String categoryName, int productCount, int totalStock, BigDecimal totalValue) {
            this.categoryName = categoryName;
            this.productCount = productCount;
            this.totalStock = totalStock;
            this.totalValue = totalValue != null ? totalValue : BigDecimal.ZERO;
        }

        public String getCategoryName() { return categoryName; }
        public int getProductCount() { return productCount; }
        public int getTotalStock() { return totalStock; }
        public BigDecimal getTotalValue() { return totalValue; }
    }

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }

    public int getTotalStockUnits() { return totalStockUnits; }
    public void setTotalStockUnits(int totalStockUnits) { this.totalStockUnits = totalStockUnits; }

    public BigDecimal getTotalInventoryValue() { return totalInventoryValue; }
    public void setTotalInventoryValue(BigDecimal totalInventoryValue) { this.totalInventoryValue = totalInventoryValue; }

    public BigDecimal getTotalRetailValue() { return totalRetailValue; }
    public void setTotalRetailValue(BigDecimal totalRetailValue) { this.totalRetailValue = totalRetailValue; }

    public int getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(int lowStockCount) { this.lowStockCount = lowStockCount; }

    public int getOutOfStockCount() { return outOfStockCount; }
    public void setOutOfStockCount(int outOfStockCount) { this.outOfStockCount = outOfStockCount; }

    public int getTotalCategories() { return totalCategories; }
    public void setTotalCategories(int totalCategories) { this.totalCategories = totalCategories; }

    public int getTotalSuppliers() { return totalSuppliers; }
    public void setTotalSuppliers(int totalSuppliers) { this.totalSuppliers = totalSuppliers; }

    public List<Product> getLowStockProducts() { return lowStockProducts; }
    public void setLowStockProducts(List<Product> lowStockProducts) { this.lowStockProducts = lowStockProducts; }

    public List<InventoryTransaction> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<InventoryTransaction> recentTransactions) { this.recentTransactions = recentTransactions; }

    public List<CategoryStat> getCategoryStats() { return categoryStats; }
    public void setCategoryStats(List<CategoryStat> categoryStats) { this.categoryStats = categoryStats; }
}
