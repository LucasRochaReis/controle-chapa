alter table app_users add column if not exists machine_id bigint;
alter table app_users
    add constraint fk_app_users_machine foreign key (machine_id) references lookup_options(id);

create index if not exists idx_app_users_machine on app_users(machine_id);

update app_users
set role = 'USUARIO'
where role = 'USER';
