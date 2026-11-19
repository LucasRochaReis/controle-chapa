alter table paper_entries add column if not exists stock_responsible varchar(100);

update paper_entries
set stock_responsible = cut_responsible
where stock_responsible is null;

alter table paper_entries alter column stock_responsible set not null;
