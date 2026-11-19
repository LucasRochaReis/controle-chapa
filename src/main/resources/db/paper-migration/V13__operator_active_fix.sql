-- V13: Garante que todos os OPERATOR cadastrados estao ativos (corrige cadastros feitos antes da correcao do active=true por padrao)
UPDATE lookup_options SET active = TRUE WHERE category = 'OPERATOR' AND active = FALSE;
