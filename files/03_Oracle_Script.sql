-- =====================================================================
-- ARTEFACTO 2B — ORACLE
-- Dominio: Sucursales, Empleados, Productos, Inventario
-- =====================================================================

-- ============ 1. USUARIO / SCHEMA (ejecutar como SYSDBA) ============
-- CREATE USER restaurante_backoffice IDENTIFIED BY "Backoffice#2026";
-- GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE PROCEDURE TO restaurante_backoffice;
-- ALTER USER restaurante_backoffice QUOTA UNLIMITED ON USERS;
-- CONNECT restaurante_backoffice/Backoffice#2026;

-- ============ 2. DDL — TABLAS ============

-- Opcional: Limpieza de objetos existentes para evitar conflictos al re-ejecutar el script
DECLARE
    PROCEDURE drop_table_if_exists(p_table_name IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'DROP TABLE ' || p_table_name || ' CASCADE CONSTRAINTS';
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE != -942 THEN
                RAISE;
            END IF;
    END;

    PROCEDURE drop_role_if_exists(p_role_name IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'DROP ROLE ' || p_role_name;
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE != -1919 THEN
                RAISE;
            END IF;
    END;

    PROCEDURE drop_user_if_exists(p_user_name IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'DROP USER ' || p_user_name || ' CASCADE';
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE != -1918 THEN
                RAISE;
            END IF;
    END;
BEGIN
    -- Eliminar tablas en orden inverso de dependencias
    drop_table_if_exists('movimiento_inventario');
    drop_table_if_exists('producto_insumo');
    drop_table_if_exists('insumo');
    drop_table_if_exists('producto');
    drop_table_if_exists('categoria');
    drop_table_if_exists('asistencia');
    drop_table_if_exists('horario');
    drop_table_if_exists('empleado');
    drop_table_if_exists('cargo');
    drop_table_if_exists('sucursal');

    -- Eliminar roles
    drop_role_if_exists('rol_admin_ora');
    drop_role_if_exists('rol_supervisor_ora');
    drop_role_if_exists('rol_cajero_ora');
    drop_role_if_exists('rol_cocinero_ora');

    -- Eliminar usuarios
    drop_user_if_exists('usr_admin');
    drop_user_if_exists('usr_supervisor');
    drop_user_if_exists('usr_cajero');
    drop_user_if_exists('usr_cocinero');
END;
/

CREATE TABLE sucursal (
    sucursal_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR2(100) NOT NULL,
    direccion       VARCHAR2(200),
    ciudad          VARCHAR2(60),
    telefono        VARCHAR2(20)
);

CREATE TABLE cargo (
    cargo_id        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_cargo    VARCHAR2(50) NOT NULL UNIQUE,
    sueldo_base     NUMBER(10,2) NOT NULL
);

CREATE TABLE empleado (
    empleado_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombres             VARCHAR2(80) NOT NULL,
    apellidos           VARCHAR2(80) NOT NULL,
    dni                 VARCHAR2(15) NOT NULL UNIQUE,
    cargo_id            NUMBER NOT NULL REFERENCES cargo(cargo_id),
    sucursal_id         NUMBER NOT NULL REFERENCES sucursal(sucursal_id),
    fecha_contratacion  DATE NOT NULL,
    estado              VARCHAR2(10) DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','INACTIVO'))
);

CREATE TABLE horario (
    horario_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empleado_id     NUMBER NOT NULL REFERENCES empleado(empleado_id),
    dia_semana      VARCHAR2(10) NOT NULL,
    hora_inicio     VARCHAR2(5)  NOT NULL,
    hora_fin        VARCHAR2(5)  NOT NULL
);

CREATE TABLE asistencia (
    asistencia_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empleado_id     NUMBER NOT NULL REFERENCES empleado(empleado_id),
    fecha           DATE NOT NULL,
    hora_entrada    VARCHAR2(5),
    hora_salida     VARCHAR2(5),
    observacion     VARCHAR2(200)
);

CREATE TABLE categoria (
    categoria_id    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_categoria VARCHAR2(50) NOT NULL UNIQUE,
    descripcion     VARCHAR2(200)
);

CREATE TABLE producto (
    producto_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_producto VARCHAR2(100) NOT NULL,
    categoria_id    NUMBER NOT NULL REFERENCES categoria(categoria_id),
    precio          NUMBER(8,2) NOT NULL,
    disponible      VARCHAR2(1) DEFAULT 'S' CHECK (disponible IN ('S','N'))
);

CREATE TABLE insumo (
    insumo_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_insumo   VARCHAR2(100) NOT NULL,
    unidad_medida   VARCHAR2(15) NOT NULL,
    stock_actual    NUMBER(10,2) DEFAULT 0,
    stock_minimo    NUMBER(10,2) DEFAULT 0
);

CREATE TABLE producto_insumo (
    producto_id         NUMBER NOT NULL REFERENCES producto(producto_id),
    insumo_id            NUMBER NOT NULL REFERENCES insumo(insumo_id),
    cantidad_requerida   NUMBER(8,2) NOT NULL,
    CONSTRAINT pk_producto_insumo PRIMARY KEY (producto_id, insumo_id)
);

CREATE TABLE movimiento_inventario (
    movimiento_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    insumo_id       NUMBER NOT NULL REFERENCES insumo(insumo_id),
    sucursal_id     NUMBER NOT NULL REFERENCES sucursal(sucursal_id),
    tipo_movimiento VARCHAR2(8) CHECK (tipo_movimiento IN ('ENTRADA','SALIDA')),
    cantidad        NUMBER(10,2) NOT NULL,
    fecha_movimiento DATE DEFAULT SYSDATE,
    motivo          VARCHAR2(200)
);

-- ============ 3. FUNCIONES Y PROCEDIMIENTOS ALMACENADOS ============

-- Procedimiento: registra una salida de inventario y actualiza el stock, con alerta
CREATE OR REPLACE PROCEDURE sp_registrar_salida_insumo (
    p_insumo_id     IN NUMBER,
    p_sucursal_id   IN NUMBER,
    p_cantidad      IN NUMBER,
    p_motivo        IN VARCHAR2
) AS
    v_stock_actual NUMBER;
    v_stock_minimo NUMBER;
BEGIN
    INSERT INTO movimiento_inventario (insumo_id, sucursal_id, tipo_movimiento, cantidad, motivo)
    VALUES (p_insumo_id, p_sucursal_id, 'SALIDA', p_cantidad, p_motivo);

    UPDATE insumo
       SET stock_actual = stock_actual - p_cantidad
     WHERE insumo_id = p_insumo_id
    RETURNING stock_actual, stock_minimo INTO v_stock_actual, v_stock_minimo;

    IF v_stock_actual <= v_stock_minimo THEN
        DBMS_OUTPUT.PUT_LINE('ALERTA: Insumo ' || p_insumo_id || ' por debajo del stock mínimo.');
    END IF;

    COMMIT;
END sp_registrar_salida_insumo;
/

-- Función: costo total de receta de un producto (suma de insumos requeridos x cantidad)
CREATE OR REPLACE FUNCTION fn_disponibilidad_producto (p_producto_id IN NUMBER)
RETURN VARCHAR2 AS
    v_faltantes NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_faltantes
    FROM producto_insumo pi
    JOIN insumo i ON i.insumo_id = pi.insumo_id
    WHERE pi.producto_id = p_producto_id
      AND i.stock_actual < pi.cantidad_requerida;

    IF v_faltantes > 0 THEN
        RETURN 'NO_DISPONIBLE';
    ELSE
        RETURN 'DISPONIBLE';
    END IF;
END fn_disponibilidad_producto;
/

-- ============ 4. DATOS DE PRUEBA (DML) ============

INSERT INTO sucursal (nombre, direccion, ciudad, telefono) VALUES
('Sede Miraflores', 'Av. Larco 123', 'Lima', '014567890');
INSERT INTO sucursal (nombre, direccion, ciudad, telefono) VALUES
('Sede San Borja', 'Av. Aviación 456', 'Lima', '014567891');

INSERT INTO cargo (nombre_cargo, sueldo_base) VALUES ('Administrador', 3500);
INSERT INTO cargo (nombre_cargo, sueldo_base) VALUES ('Supervisor', 2800);
INSERT INTO cargo (nombre_cargo, sueldo_base) VALUES ('Cajero', 1500);
INSERT INTO cargo (nombre_cargo, sueldo_base) VALUES ('Cocinero', 1800);

INSERT INTO empleado (nombres, apellidos, dni, cargo_id, sucursal_id, fecha_contratacion) VALUES
('Luis', 'Ramirez', '70001122', 1, 1, DATE '2024-03-01');
INSERT INTO empleado (nombres, apellidos, dni, cargo_id, sucursal_id, fecha_contratacion) VALUES
('Maria', 'Castillo', '70002233', 3, 1, DATE '2024-05-15');
INSERT INTO empleado (nombres, apellidos, dni, cargo_id, sucursal_id, fecha_contratacion) VALUES
('Jorge', 'Salas', '70003344', 4, 1, DATE '2024-06-01');
INSERT INTO empleado (nombres, apellidos, dni, cargo_id, sucursal_id, fecha_contratacion) VALUES
('Paola', 'Reyes', '70004455', 3, 2, DATE '2024-07-10');

INSERT INTO categoria (nombre_categoria, descripcion) VALUES ('Entrada', 'Platos de entrada');
INSERT INTO categoria (nombre_categoria, descripcion) VALUES ('Fondo', 'Platos de fondo');
INSERT INTO categoria (nombre_categoria, descripcion) VALUES ('Bebida', 'Bebidas frias y calientes');

INSERT INTO producto (nombre_producto, categoria_id, precio) VALUES ('Causa Limeña', 1, 18.00);
INSERT INTO producto (nombre_producto, categoria_id, precio) VALUES ('Lomo Saltado', 2, 25.50);
INSERT INTO producto (nombre_producto, categoria_id, precio) VALUES ('Chicha Morada', 3, 12.00);
INSERT INTO producto (nombre_producto, categoria_id, precio) VALUES ('Aji de Gallina', 2, 35.00);

INSERT INTO insumo (nombre_insumo, unidad_medida, stock_actual, stock_minimo) VALUES ('Papa', 'kg', 50, 10);
INSERT INTO insumo (nombre_insumo, unidad_medida, stock_actual, stock_minimo) VALUES ('Carne de Res', 'kg', 30, 5);
INSERT INTO insumo (nombre_insumo, unidad_medida, stock_actual, stock_minimo) VALUES ('Maiz Morado', 'kg', 15, 3);

INSERT INTO producto_insumo (producto_id, insumo_id, cantidad_requerida) VALUES (1, 1, 0.3);
INSERT INTO producto_insumo (producto_id, insumo_id, cantidad_requerida) VALUES (2, 2, 0.25);
INSERT INTO producto_insumo (producto_id, insumo_id, cantidad_requerida) VALUES (3, 3, 0.1);

COMMIT;

-- Ejemplo de uso de procedimiento
-- EXEC sp_registrar_salida_insumo(1, 1, 5, 'Consumo dia 24/06');

-- ============ 5. ROLES Y PERMISOS (Administración y Seguridad) ============

CREATE ROLE rol_admin_ora;
CREATE ROLE rol_supervisor_ora;
CREATE ROLE rol_cajero_ora;
CREATE ROLE rol_cocinero_ora;

GRANT ALL ON sucursal       TO rol_admin_ora;
GRANT ALL ON empleado       TO rol_admin_ora;
GRANT ALL ON producto       TO rol_admin_ora;
GRANT ALL ON insumo         TO rol_admin_ora;

GRANT SELECT ON sucursal TO rol_supervisor_ora;
GRANT SELECT ON empleado TO rol_supervisor_ora;
GRANT SELECT ON producto TO rol_supervisor_ora;
GRANT SELECT ON insumo TO rol_supervisor_ora;
GRANT SELECT ON movimiento_inventario TO rol_supervisor_ora;

GRANT SELECT ON producto TO rol_cajero_ora;
GRANT SELECT ON categoria TO rol_cajero_ora;

GRANT SELECT ON producto TO rol_cocinero_ora;
GRANT SELECT ON insumo TO rol_cocinero_ora;
GRANT SELECT ON producto_insumo TO rol_cocinero_ora;
GRANT INSERT, UPDATE ON movimiento_inventario TO rol_cocinero_ora;
GRANT EXECUTE ON sp_registrar_salida_insumo TO rol_cocinero_ora;

-- Usuarios de ejemplo asociados a cada rol
CREATE USER usr_admin     IDENTIFIED BY "Admin#2026Secure";
CREATE USER usr_supervisor IDENTIFIED BY "Super#2026Secure";
CREATE USER usr_cajero    IDENTIFIED BY "Cajero#2026Secure";
CREATE USER usr_cocinero  IDENTIFIED BY "Cocinero#2026Secure";

GRANT CONNECT TO usr_admin, usr_supervisor, usr_cajero, usr_cocinero;
GRANT rol_admin_ora       TO usr_admin;
GRANT rol_supervisor_ora  TO usr_supervisor;
GRANT rol_cajero_ora      TO usr_cajero;
GRANT rol_cocinero_ora    TO usr_cocinero;

-- ============ 6. BACKUP Y RESTORE (Oracle Cloud / Autonomous DB) ============

-- NOTA: En Oracle Cloud, la conexión requiere descargar el archivo "Wallet" (.zip)
-- desde el panel web de Oracle y usar la contraseña de tu usuario administrador.

-- ============ 7. CONSULTAS RELEVANTES DE NEGOCIO (SELECTs) ============

-- Consulta 1: Alerta de Insumos Críticos con Stock por Debajo o Igual al Mínimo (Reposición)
SELECT 
    i.insumo_id,
    i.nombre_insumo,
    i.unidad_medida,
    i.stock_actual,
    i.stock_minimo,
    (i.stock_minimo - i.stock_actual) AS cantidad_a_reponer,
    CASE 
        WHEN i.stock_actual = 0 THEN 'AGOTADO'
        WHEN i.stock_actual <= i.stock_minimo THEN 'ALERTA REPOSICION'
        ELSE 'OK'
    END AS estado_stock
FROM insumo i
WHERE i.stock_actual <= i.stock_minimo
ORDER BY i.stock_actual ASC;

-- Consulta 2: Catálogo de Productos con Categoría, Precio y Estado de Disponibilidad por Insumos
SELECT 
    p.producto_id,
    p.nombre_producto,
    c.nombre_categoria,
    p.precio,
    fn_disponibilidad_producto(p.producto_id) AS estado_receta,
    COUNT(pi.insumo_id) AS total_insumos_requeridos
FROM producto p
JOIN categoria c ON p.categoria_id = c.categoria_id
LEFT JOIN producto_insumo pi ON p.producto_id = pi.producto_id
GROUP BY p.producto_id, p.nombre_producto, c.nombre_categoria, p.precio
ORDER BY c.nombre_categoria, p.precio DESC;


