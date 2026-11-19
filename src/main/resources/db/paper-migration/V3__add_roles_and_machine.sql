alter table app_users add column if not exists role varchar(20);
update app_users set role = 'USER' where role is null;
update app_users set role = 'ADMIN' where lower(username) = 'admin';
alter table app_users alter column role set not null;

alter table paper_entries add column if not exists machine_id bigint;
alter table paper_entries
    add constraint fk_paper_entries_machine foreign key (machine_id) references lookup_options(id);

create index if not exists idx_paper_entries_machine on paper_entries(machine_id);

insert into lookup_options (category, label, active, sort_order) values
    ('MACHINE', 'Guilhotina 1', true, 1),
    ('MACHINE', 'Guilhotina 2', true, 2),
    ('MACHINE', 'Guilhotina 3', true, 3),
    ('MACHINE', 'Guilhotina 4', true, 4)
on conflict (category, label) do nothing;
