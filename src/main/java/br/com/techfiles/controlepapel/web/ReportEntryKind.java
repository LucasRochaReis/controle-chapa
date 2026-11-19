package br.com.techfiles.controlepapel.web;

public enum ReportEntryKind {
    ALL("Todos"),
    PLATE_PRODUCTION("Lançamento"),
    PLATE_DEFECT("Ocorrências de Chapa");

    private final String label;

    ReportEntryKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ReportEntryKind fromParam(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ALL;
        }
        for (ReportEntryKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return ALL;
    }
}
