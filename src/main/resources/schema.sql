CREATE SCHEMA IF NOT EXISTS salesmaster;

SET search_path TO salesmaster;



-- CLIENTE

CREATE TABLE IF NOT EXISTS cliente (

 id_cliente SERIAL PRIMARY KEY,

 nombre VARCHAR(80) NOT NULL,

 email VARCHAR(100) UNIQUE NOT NULL

);



-- PRODUCTO

CREATE TABLE IF NOT EXISTS producto (

 id_prod SERIAL PRIMARY KEY,

 nombre VARCHAR(80) NOT NULL,

 precio NUMERIC(10,2) NOT NULL

);



-- PEDIDO

CREATE TABLE IF NOT EXISTS pedido (

 id_pedido SERIAL PRIMARY KEY,

 id_cliente INT REFERENCES cliente(id_cliente),

 fecha TIMESTAMP NOT NULL,

 total NUMERIC(10,2)

);



-- PEDIDO_PRODUCTO (relación N:M)

CREATE TABLE IF NOT EXISTS pedido_producto (

 id_pedido INT REFERENCES pedido(id_pedido),

 id_prod INT REFERENCES producto(id_prod),

 cantidad INT NOT NULL,

 subtotal NUMERIC(10,2) NOT NULL,

 PRIMARY KEY(id_pedido, id_prod)

);



-- FACTURA

CREATE TABLE IF NOT EXISTS factura (

 id_factura SERIAL PRIMARY KEY,

 id_pedido INT REFERENCES pedido(id_pedido),

 nro VARCHAR(15) NOT NULL,

 fecha TIMESTAMP NOT NULL,

 total NUMERIC(10,2)

);

