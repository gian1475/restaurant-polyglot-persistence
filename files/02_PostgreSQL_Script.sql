-- =====================================================================
-- ARTEFACTO 2A — POSTGRESQL
-- Dominio: Clientes, Fidelización, Reservas, Pedidos, Pagos
-- =====================================================================

-- ============ 1. CREACIÓN DE BASE DE DATOS ============
-- (ejecutar conectado a la BD 'postgres' como superusuario)
-- CREATE DATABASE restaurante_frontoffice
--     WITH ENCODING 'UTF8' LC_COLLATE 'es_PE.UTF-8' LC_CTYPE 'es_PE.UTF-8';

-- \c restaurante_frontoffice

-- ============ 2. DDL — TABLAS ============

CREATE TABLE cliente (
    cliente_id      SERIAL PRIMARY KEY,
    nombres         VARCHAR(80)  NOT NULL,
    apellidos       VARCHAR(80)  NOT NULL,
    email           VARCHAR(120) NOT NULL UNIQUE,
    telefono        VARCHAR(20),
    fecha_registro  DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE metodo_pago (
    metodo_pago_id  SERIAL PRIMARY KEY,
    nombre_metodo   VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE reserva (
    reserva_id      SERIAL PRIMARY KEY,
    cliente_id      INTEGER NOT NULL REFERENCES cliente(cliente_id),
    sucursal_id     INTEGER NOT NULL, -- FK lógica -> ORACLE.SUCURSAL (validada en Java)
    fecha_reserva   DATE NOT NULL,
    hora_reserva    TIME NOT NULL,
    num_personas    SMALLINT NOT NULL CHECK (num_personas > 0),
    estado          VARCHAR(15) NOT NULL DEFAULT 'CONFIRMADA'
                    CHECK (estado IN ('CONFIRMADA','CANCELADA','COMPLETADA'))
);

CREATE TABLE pedido (
    pedido_id           SERIAL PRIMARY KEY,
    cliente_id          INTEGER NOT NULL REFERENCES cliente(cliente_id),
    empleado_id         INTEGER NOT NULL, -- FK lógica -> ORACLE.EMPLEADO
    sucursal_id         INTEGER NOT NULL, -- FK lógica -> ORACLE.SUCURSAL
    tipo_pedido         VARCHAR(10) NOT NULL CHECK (tipo_pedido IN ('SALON','DELIVERY')),
    fecha_pedido        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_pedido       VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado_pedido IN ('PENDIENTE','PREPARACION','ENTREGADO','CANCELADO')),
    direccion_delivery  VARCHAR(200)
);

CREATE TABLE detalle_pedido (
    detalle_id      SERIAL PRIMARY KEY,
    pedido_id       INTEGER NOT NULL REFERENCES pedido(pedido_id) ON DELETE CASCADE,
    producto_id     INTEGER NOT NULL, -- FK lógica -> ORACLE.PRODUCTO
    cantidad        SMALLINT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(8,2) NOT NULL, -- snapshot histórico, no redundancia
    subtotal        NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED
);

CREATE TABLE pago (
    pago_id             SERIAL PRIMARY KEY,
    pedido_id           INTEGER NOT NULL UNIQUE REFERENCES pedido(pedido_id),
    metodo_pago_id      INTEGER NOT NULL REFERENCES metodo_pago(metodo_pago_id),
    monto               NUMERIC(10,2) NOT NULL,
    fecha_pago          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    numero_comprobante  VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE fidelizacion_movimiento (
    movimiento_id   SERIAL PRIMARY KEY,
    cliente_id      INTEGER NOT NULL REFERENCES cliente(cliente_id),
    tipo            VARCHAR(12) NOT NULL CHECK (tipo IN ('ACUMULACION','REDENCION')),
    puntos          INTEGER NOT NULL,
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pedido_id       INTEGER REFERENCES pedido(pedido_id)
);

-- Vista: puntos vigentes por cliente (evita guardar un total redundante)
CREATE VIEW vw_puntos_cliente AS
SELECT c.cliente_id, c.nombres, c.apellidos,
       COALESCE(SUM(CASE WHEN f.tipo = 'ACUMULACION' THEN f.puntos ELSE -f.puntos END), 0) AS puntos_vigentes
FROM cliente c
LEFT JOIN fidelizacion_movimiento f ON f.cliente_id = c.cliente_id
GROUP BY c.cliente_id, c.nombres, c.apellidos;

-- Vista: ventas por sucursal (usada también para comparar contra Hadoop)
CREATE VIEW vw_ventas_pedido AS
SELECT p.pedido_id, p.sucursal_id, p.fecha_pedido, pa.monto
FROM pedido p
JOIN pago pa ON pa.pedido_id = p.pedido_id;

-- ============ 3. FUNCIONES Y PROCEDIMIENTOS ALMACENADOS ============

-- Función: registra un pedido con su detalle de forma atómica
CREATE OR REPLACE FUNCTION fn_registrar_pedido(
    p_cliente_id INTEGER,
    p_empleado_id INTEGER,
    p_sucursal_id INTEGER,
    p_tipo VARCHAR,
    p_productos INTEGER[],      -- ids de producto
    p_cantidades SMALLINT[],
    p_precios NUMERIC[]
) RETURNS INTEGER AS $$
DECLARE
    v_pedido_id INTEGER;
    i INTEGER;
BEGIN
    INSERT INTO pedido(cliente_id, empleado_id, sucursal_id, tipo_pedido)
    VALUES (p_cliente_id, p_empleado_id, p_sucursal_id, p_tipo)
    RETURNING pedido_id INTO v_pedido_id;

    FOR i IN 1 .. array_length(p_productos, 1) LOOP
        INSERT INTO detalle_pedido(pedido_id, producto_id, cantidad, precio_unitario)
        VALUES (v_pedido_id, p_productos[i], p_cantidades[i], p_precios[i]);
    END LOOP;

    RETURN v_pedido_id;
END;
$$ LANGUAGE plpgsql;

-- Procedimiento: acumular puntos automáticamente al registrar un pago (1 punto por cada S/10)
CREATE OR REPLACE FUNCTION trg_fn_acumular_puntos() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO fidelizacion_movimiento(cliente_id, tipo, puntos, pedido_id)
    SELECT p.cliente_id, 'ACUMULACION', FLOOR(NEW.monto / 10), NEW.pedido_id
    FROM pedido p WHERE p.pedido_id = NEW.pedido_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_acumular_puntos
AFTER INSERT ON pago
FOR EACH ROW EXECUTE FUNCTION trg_fn_acumular_puntos();

-- ============ 4. DATOS DE PRUEBA (DML) ============

INSERT INTO metodo_pago (nombre_metodo) VALUES
('Efectivo'), ('Tarjeta'), ('Yape'), ('Plin'), ('Transferencia');

INSERT INTO cliente (nombres, apellidos, email, telefono) VALUES
('Gianella', 'Torres', 'gianella.torres@mail.com', '987654321'),
('Renzo', 'Quispe', 'renzo.quispe@mail.com', '912345678'),
('Camila', 'Flores', 'camila.flores@mail.com', '998877665');

INSERT INTO reserva (cliente_id, sucursal_id, fecha_reserva, hora_reserva, num_personas) VALUES
(1, 1, '2026-07-01', '19:30', 4),
(2, 2, '2026-07-02', '20:00', 2);


-- Pedido + detalle vía la función (con los tipos de datos casteados explícitamente)
SELECT fn_registrar_pedido(1, 3, 1, 'SALON'::VARCHAR, ARRAY[10,11], ARRAY[2,1]::SMALLINT[], ARRAY[25.50, 12.00]);
SELECT fn_registrar_pedido(2, 4, 2, 'DELIVERY'::VARCHAR, ARRAY[12], ARRAY[1]::SMALLINT[], ARRAY[35.00]);

INSERT INTO pago (pedido_id, metodo_pago_id, monto, numero_comprobante) VALUES
(1, 2, 63.00, 'F001-000123'),
(2, 3, 35.00, 'F001-000124');

-- ============ 5. ROLES Y PERMISOS (Administración y Seguridad) ============

CREATE ROLE rol_admin       LOGIN PASSWORD 'Admin#2026';
CREATE ROLE rol_supervisor  LOGIN PASSWORD 'Super#2026';
CREATE ROLE rol_cajero      LOGIN PASSWORD 'Cajero#2026';
CREATE ROLE rol_cocinero    LOGIN PASSWORD 'Cocinero#2026';

-- Admin: control total sobre el esquema
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO rol_admin;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO rol_admin;

-- Supervisor: lectura total + escritura en reportes/vistas
GRANT SELECT ON ALL TABLES IN SCHEMA public TO rol_supervisor;

-- Cajero: gestiona pedidos y pagos, no toca clientes/fidelización a nivel estructural
GRANT SELECT, INSERT, UPDATE ON pedido, detalle_pedido, pago TO rol_cajero;
GRANT SELECT ON cliente, metodo_pago TO rol_cajero;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO rol_cajero;

-- Cocinero: solo necesita ver pedidos para preparar, no pagos ni clientes
GRANT SELECT, UPDATE (estado_pedido) ON pedido TO rol_cocinero;
GRANT SELECT ON detalle_pedido TO rol_cocinero;

-- ============ 6. BACKUP Y RESTORE (ejecutar desde terminal, NO desde psql) ============

-- Backup completo (formato comprimido custom):
-- pg_dump "postgresql://<USUARIO>:<PASSWORD>@<HOST_POSTGRES>:5432/neondb?sslmode=require" -F c -b -v -f "backup_restaurante_frontoffice.backup"

-- Backup en SQL plano (más fácil de inspeccionar para tu informe):
-- pg_dump "postgresql://<USUARIO>:<PASSWORD>@<HOST_POSTGRES>:5432/neondb?sslmode=require" -F p -f "backup_restaurante_frontoffice.sql"

-- Restore (formato custom):
-- pg_restore -d "postgresql://<USUARIO>:<PASSWORD>@<HOST_POSTGRES>:5432/neondb?sslmode=require" -v "backup_restaurante_frontoffice.backup"

-- Restore (formato plano):
-- psql "postgresql://<USUARIO>:<PASSWORD>@<HOST_POSTGRES>:5432/neondb?sslmode=require" -f "backup_restaurante_frontoffice.sql"

-- ============ 7. CONSULTAS RELEVANTES DE NEGOCIO (SELECTs) ============

-- Consulta 1: Reporte de Clientes Frecuentes, Total Consumido y Puntos de Fidelización
SELECT 
    c.cliente_id,
    c.nombres || ' ' || c.apellidos AS cliente,
    c.email,
    COUNT(DISTINCT p.pedido_id) AS total_pedidos,
    COALESCE(SUM(pa.monto), 0) AS total_gastado_soles,
    COALESCE(vp.puntos_vigentes, 0) AS puntos_acumulados
FROM cliente c
LEFT JOIN pedido p ON c.cliente_id = p.cliente_id
LEFT JOIN pago pa ON p.pedido_id = pa.pedido_id
LEFT JOIN vw_puntos_cliente vp ON c.cliente_id = vp.cliente_id
GROUP BY c.cliente_id, c.nombres, c.apellidos, c.email, vp.puntos_vigentes
ORDER BY total_gastado_soles DESC;

-- Consulta 2: Ingresos Totales por Método de Pago y Canal de Venta (Salón vs Delivery)
SELECT 
    p.tipo_pedido,
    mp.nombre_metodo AS metodo_pago,
    COUNT(p.pedido_id) AS cantidad_pedidos,
    COALESCE(SUM(pa.monto), 0) AS total_recaudado
FROM pedido p
JOIN pago pa ON p.pedido_id = pa.pedido_id
JOIN metodo_pago mp ON pa.metodo_pago_id = mp.metodo_pago_id
GROUP BY p.tipo_pedido, mp.nombre_metodo
ORDER BY total_recaudado DESC;


