package br.com.techfiles.controlepapel.model;

public enum LookupCategory {
    SHIFT("Turno"),
    MACHINE("Maquina"),
    PLATE("Chapa"),
    CTP("CTP"),
    DEFECT_CODE("Codigo de ocorrencia"),
    OPERATOR("Operador");

    private final String label;

    LookupCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
