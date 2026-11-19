package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.LookupCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LookupOptionForm {

    private Long id;

    @NotNull(message = "Selecione o tipo de cadastro.")
    private LookupCategory category;

    @NotBlank(message = "Informe a descricao.")
    private String label;

    private boolean active = true;

    public LookupCategory getCategory() {
        return category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCategory(LookupCategory category) {
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
