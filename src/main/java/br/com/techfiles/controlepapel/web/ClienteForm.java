package br.com.techfiles.controlepapel.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClienteForm {

    private Long id;

    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 150, message = "O nome do cliente deve ter no maximo 150 caracteres.")
    private String nome;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
