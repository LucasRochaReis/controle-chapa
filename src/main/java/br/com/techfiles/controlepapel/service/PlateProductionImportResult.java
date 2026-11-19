package br.com.techfiles.controlepapel.service;

import java.util.ArrayList;
import java.util.List;

public class PlateProductionImportResult {

    private int totalRows;
    private int successCount;
    private int errorCount;
    private final List<String> errors;

    public PlateProductionImportResult() {
        this.errors = new ArrayList<>();
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(int rowNumber, String message) {
        this.errorCount++;
        this.errors.add("Linha " + rowNumber + ": " + message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
