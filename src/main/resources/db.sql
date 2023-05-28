CREATE TABLE airplane (
    id serial primary key not null ,
    brand varchar(50),
    model varchar(50),
    number_of_seats int
);

CREATE TABLE ship (
    id serial primary key not null ,
    brand varchar(50),
    model varchar(50),
    max_cargo int
);