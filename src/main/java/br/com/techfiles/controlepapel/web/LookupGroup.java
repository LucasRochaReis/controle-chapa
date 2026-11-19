package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;

import java.util.List;

/**
 * Agrupa os valores selecionaveis de um mesmo cadastro (categoria),
 * para exibir a pagina de Cadastros separada por tipo.
 */
public record LookupGroup(LookupCategory category, List<LookupOption> options) {
}
