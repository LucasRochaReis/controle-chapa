-- Remove tabelas descontinuadas de Controle de Papel Branco e Operacionais,
-- alem dos registros de lookup associados a essas categorias (nao sao mais usadas no enumarado LookupCategory).

drop table if exists operational_entries cascade;
drop table if exists paper_entries cascade;

delete from lookup_options where category in ('PAPER_TYPE', 'PAPER_FORMAT', 'PAPER_GRAMMAGE');
