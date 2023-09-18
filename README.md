# projector-sharding

# Project structure

- [script to create statements for insert and select](./app/src/main/kotlin/com/jdum/projector/sharding/Application.kt)
- [docker compose file for postgres FWD](./docker-compose-fwd.yml)
- [docker compose file for single postgres node](./docker-compose-single.yml)
- [docker compose file for citus single node](./docker-compose-citus-single.yml)

# FWD Sharding

1. Run [docker compose file for postgres FWD](./docker-compose-fwd.yml)
2. Create tables on shards:

```postgresql
CREATE TABLE if not exists books
(
    id          SERIAL PRIMARY KEY,
    category_id int         not null,
    CONSTRAINT category_id_check CHECK ( category_id >= 1 AND category_id < 10 ),
    title       VARCHAR(50) NOT NULL
);
```

```postgresql
CREATE TABLE if not exists books
(
    id          SERIAL PRIMARY KEY,
    category_id int         not null,
    CONSTRAINT category_id_check CHECK ( category_id >= 10 AND category_id < 20 ),
    title       VARCHAR(50) NOT NULL
);
```

3. Configure master DB

```postgresql
CREATE EXTENSION postgres_fdw;

CREATE SERVER books_1_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS ( host 'postgresql-b1', port '5432', dbname 'postgres' );

CREATE USER MAPPING FOR "postgres"
    SERVER books_1_server
    OPTIONS (user 'postgres', password 'postgres');

CREATE SERVER books_2_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS ( host 'postgresql-b2', port '5432', dbname 'postgres' );

CREATE USER MAPPING FOR "postgres"
    SERVER books_2_server
    OPTIONS (user 'postgres', password 'postgres');

CREATE FOREIGN TABLE books_1
    (
        id SERIAL not null,
        category_id int not null,
        title VARCHAR(50) NOT NULL
        )
    SERVER books_1_server
    OPTIONS (schema_name 'public', table_name 'books');

CREATE FOREIGN TABLE books_2
    (
        id SERIAL not null,
        category_id int not null,
        title VARCHAR(50) NOT NULL
        )
    SERVER books_2_server
    OPTIONS (schema_name 'public', table_name 'books');

CREATE VIEW books AS
SELECT *
FROM books_1
UNION ALL
SELECT *
FROM books_2;

CREATE RULE books_insert AS ON INSERT TO books
    DO INSTEAD NOTHING;
CREATE RULE books_update AS ON UPDATE TO books
    DO INSTEAD NOTHING;
CREATE RULE books_delete AS ON DELETE TO books
    DO INSTEAD NOTHING;

CREATE RULE books_insert_to_1 AS ON INSERT TO books
    WHERE (category_id >= 1 AND category_id < 10)
    DO INSTEAD INSERT INTO books_1
               VALUES (NEW.*);

CREATE RULE books_insert_to_2 AS ON INSERT TO books
    WHERE (category_id >= 10 AND category_id < 20)
    DO INSTEAD INSERT INTO books_2
               VALUES (NEW.*);
```

4. Insert [data](./data_insert.sql)
5. Run select [queries](./data_select.sql)

# Postgres Single Node

1. Run [docker compose file for single postgres node](./docker-compose-single.yml)
2. Create a table:

```postgresql
CREATE TABLE if not exists books
(
    id          SERIAL PRIMARY KEY,
    category_id int         not null,
    title       VARCHAR(50) NOT NULL
);
```

3. Insert [data](./data_insert.sql)
4. Run select [queries](./data_select.sql)

# Citus Single Node

1. Run [docker compose file for citus single node](./docker-compose-citus-single.yml)
2. Create a table:

```postgresql
CREATE TABLE if not exists books
(
    id          SERIAL PRIMARY KEY,
    category_id int         not null,
    title       VARCHAR(50) NOT NULL
);
```

3. Insert [data](./data_insert.sql)
4. Run select [queries](./data_select.sql)

# Citus Multi Node

I could not set up partition by category_id field due to error that category_id should be included in PK.
Therefore, let's partition by id: first 1/2 items will be kept in 1 table, another 1/2 items - in another table.

1. Run [docker compose file for citus multi node](./docker-compose-citus-multi.yml)
2. Login to master table and run:
```postgresql
CREATE TABLE books
(
    id          SERIAL PRIMARY KEY,
    category_id INT,
    title       VARCHAR(50) NOT NULL
) partition by range (id);

CREATE TABLE books_0_50000 PARTITION OF books FOR VALUES FROM (1) TO (50000);
CREATE TABLE books_50001_1000000 PARTITION OF books FOR VALUES FROM (50001) TO (1000000);
```
3. Insert [data](./data_insert.sql)
4. Run select [queries](./data_select.sql)

# Comparison Table

| Mode                 | Insert 1M rows, ms | Select 10000 rows,ms |
|----------------------|--------------------|----------------------|
| FWD                  | 169,971            | 45,117               |
| Single Postgres Node | 32,284             | 23,434               |
| Citus Single Node    | 32,706             | 26,075               |
| Citus Multi Node     | 55,266             | 27,726               |

