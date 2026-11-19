package br.com.techfiles.controlepapel.web;

import java.math.BigDecimal;

public record DashboardData(
        long totalLancamentos,
        long totalProducoes,
        long totalOcorrencias,
        long totalChapasProduzidas,
        long totalChapasPerdidas,
        BigDecimal percentualProducoes,
        BigDecimal percentualOcorrencias,
        BigDecimal percentualPerdas,
        BigDecimal percentualProduzidas,
        int diasAnalisados,
        int progressoPerdaPct,
        int perdaPctInteiro,
        int ocorrenciaPctInteiro,
        String nomeMesAno,
        String mesValor,
        String totalLancamentosFmt,
        String totalProducoesFmt,
        String totalOcorrenciasFmt,
        String totalChapasProduzidasFmt,
        String totalChapasPerdidasFmt,
        String percentualProducoesFmt,
        String percentualOcorrenciasFmt,
        String percentualPerdasFmt,
        String percentualProduzidasFmt,
        String perdaCentroDonutFmt
) {
}
