delete from lookup_options where category in ('PAPER_FORMAT', 'PAPER_GRAMMAGE', 'PAPER_TYPE');

insert into lookup_options (category, label, active, sort_order) values
    ('PAPER_FORMAT', '64x88', true, 1),
    ('PAPER_FORMAT', '66x96', true, 2),
    ('PAPER_FORMAT', '72x102', true, 3),
    ('PAPER_FORMAT', '76x112', true, 4),
    ('PAPER_FORMAT', '80x117', true, 5);

insert into lookup_options (category, label, active, sort_order) values
    ('PAPER_GRAMMAGE', '65', true, 1),
    ('PAPER_GRAMMAGE', '70', true, 2),
    ('PAPER_GRAMMAGE', '80', true, 3),
    ('PAPER_GRAMMAGE', '115', true, 4),
    ('PAPER_GRAMMAGE', '150', true, 5),
    ('PAPER_GRAMMAGE', '170', true, 6),
    ('PAPER_GRAMMAGE', '180', true, 7),
    ('PAPER_GRAMMAGE', '210', true, 8),
    ('PAPER_GRAMMAGE', '250', true, 9),
    ('PAPER_GRAMMAGE', '300', true, 10),
    ('PAPER_GRAMMAGE', '350', true, 11);

insert into lookup_options (category, label, active, sort_order) values
    ('PAPER_TYPE', 'OFFSET', true, 1),
    ('PAPER_TYPE', 'C. FOSCO', true, 2),
    ('PAPER_TYPE', 'SUPREMO', true, 3),
    ('PAPER_TYPE', 'TRIPLEX', true, 4),
    ('PAPER_TYPE', 'DUPLEX', true, 5),
    ('PAPER_TYPE', 'PS', true, 6),
    ('PAPER_TYPE', 'PP', true, 7),
    ('PAPER_TYPE', 'PVC', true, 8);
