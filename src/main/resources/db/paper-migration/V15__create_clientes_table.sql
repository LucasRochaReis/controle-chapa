create table if not exists clientes (
    id bigserial primary key,
    nome varchar(150) not null,
    criado_em timestamp not null default current_timestamp,
    atualizado_em timestamp not null default current_timestamp,
    constraint uk_clientes_nome unique (nome)
);

create index if not exists idx_clientes_nome on clientes (nome);
