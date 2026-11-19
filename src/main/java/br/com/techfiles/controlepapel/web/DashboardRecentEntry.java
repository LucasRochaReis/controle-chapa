package br.com.techfiles.controlepapel.web;

import java.time.LocalDateTime;

public record DashboardRecentEntry(
        Long id,
        String tipo,
        LocalDateTime dataHora,
        String dataFormatada,
        String chapa,
        String ctp,
        String maquina,
        Integer quantidade,
        Integer perdas,
        String usuario,
        String detalhe,
        String url
) {
}
