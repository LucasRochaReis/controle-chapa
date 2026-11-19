package br.com.techfiles.controlepapel.model;

public enum AppUserRole {
    ADMIN("Administrador"),
    GESTOR("Gestor"),
    VISUALIZADOR("Visualizador"),
    USUARIO("Usuario");

    private final String label;

    AppUserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
