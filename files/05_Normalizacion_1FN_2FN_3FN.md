# Proceso de Normalización y Diseño del Modelo Relacional

**Curso:** Base de Datos II  
**Especialidad:** Ingeniería de Sistemas / Ingeniería de Software  

---

## 0. Introducción y Enfoque Metodológico

El diseño de una base de datos relacional requiere un proceso estructurado para **garantizar la consistencia de los datos, mitigar redundancias y eliminar anomalías de inserción, actualización y borrado (Codd, 1970)**. 

El presente documento expone la normalización completa del **Sistema de Gestión para una Cadena de Restaurantes**, cuyo diseño físico final se encuentra implementado de manera distribuida: el dominio de Back-Office en **Oracle** y el dominio de Front-Office en **PostgreSQL**.

### Metodología de análisis:
1. **0FN (Relación Universal):** Modelado conceptual de la planilla desnormalizada de partida. Representada de forma **transpuesta (vertical)** para permitir una lectura óptima y evitar el desbordamiento horizontal en el informe.
2. **1FN (Primera Forma Normal):** Garantizar la atomicidad de los datos y la eliminación de grupos repetitivos (multivalores) mediante la separación de entidades y la definición de claves primarias (PK) claras.
3. **2FN (Segunda Forma Normal):** Identificación y eliminación de **dependencias parciales** (atributos no clave que dependen de una fracción de una clave primaria compuesta).
   > *Nota:* Es un error conceptual frecuente clasificar la separación de tablas con claves simples en 2FN. Cualquier tabla con una clave primaria simple (de un solo atributo) se encuentra en 2FN de forma automática.
4. **3FN (Tercera Forma Normal):** Identificación y eliminación de **dependencias transitivas** (atributos no clave que determinan funcionalmente a otros atributos no clave).

---

## 1. Dominio Back-Office — Oracle

### 1.1 Relación Universal (0FN — Sin normalizar)

Se asume la existencia de una sola tabla relacional plana que almacena la información operacional y de catálogo.

#### Tabla 1.1: Relación Universal Oracle (0FN Transpuesta)

| Atributo (Columna)            | Registro de Ejemplo 1      | Registro de Ejemplo 2      | Registro de Ejemplo 3      |
| :---------------------------- | :------------------------- | :------------------------- | :------------------------- |
| **sucursal_id**               | 1                          | 1                          | 2                          |
| **nombre_sucursal**           | Sede Miraflores            | Sede Miraflores            | Sede San Borja             |
| **direccion_sucursal**        | Av. Larco 123              | Av. Larco 123              | Av. Aviación 456           |
| **ciudad**                    | Lima                       | Lima                       | Lima                       |
| **telefono_sucursal**         | 014567890                  | 014567890                  | 014567891                  |
| **empleado_id**               | 1                          | 2                          | 4                          |
| **nombres_emp**               | Luis                       | Maria                      | Paola                      |
| **apellidos_emp**             | Ramirez                    | Castillo                   | Reyes                      |
| **dni**                       | 70001122                   | 70002233                   | 70004455                   |
| **nombre_cargo**              | Administrador              | Cajero                     | Cajero                     |
| **sueldo_base**               | 3500.00                    | 1500.00                    | 1500.00                    |
| **fecha_contratacion**        | 2024-03-01                 | 2024-05-15                 | 2024-07-10                 |
| **estado_empleado**           | ACTIVO                     | ACTIVO                     | ACTIVO                     |
| **horario_id**                | 1                          | 3                          | *[nulo]*                   |
| **dia_semana**                | Lunes                      | Lunes                      | *[nulo]*                   |
| **hora_inicio**               | 08:00                      | 09:00                      | *[nulo]*                   |
| **hora_fin**                  | 17:00                      | 18:00                      | *[nulo]*                   |
| **asistencia_id**             | 101                        | *[nulo]*                   | *[nulo]*                   |
| **fecha_asistencia**          | 2026-07-01                 | *[nulo]*                   | *[nulo]*                   |
| **hora_entrada**              | 07:55                      | *[nulo]*                   | *[nulo]*                   |
| **hora_salida**               | 17:05                      | *[nulo]*                   | *[nulo]*                   |
| **observacion_asistencia**    | PUNTUAL                    | *[nulo]*                   | *[nulo]*                   |
| **categoria_id**              | 1                          | 2                          | 3                          |
| **nombre_categoria**          | Entrada                    | Fondo                      | Bebida                     |
| **descripcion_cat**           | Platos de entrada          | Platos de fondo            | Bebidas frias y calientes  |
| **producto_id**               | 1                          | 2                          | 3                          |
| **nombre_producto**           | Causa Limeña               | Lomo Saltado               | Chicha Morada              |
| **precio_producto**           | 18.00                      | 25.50                      | 12.00                      |
| **disponible**                | S                          | S                          | S                          |
| **insumo_id**                 | 1                          | 2                          | 3                          |
| **nombre_insumo**             | Papa                       | Carne de Res               | Maiz Morado                |
| **unidad_medida**             | kg                         | kg                         | kg                         |
| **stock_actual**              | 50.00                      | 30.00                      | 15.00                      |
| **stock_minimo**              | 10.00                      | 5.00                       | 3.00                       |
| **cantidad_requerida**        | 0.30                       | 0.25                       | 0.10                       |
| **movimiento_id**             | 1                          | *[nulo]*                   | *[nulo]*                   |
| **tipo_movimiento**           | SALIDA                     | *[nulo]*                   | *[nulo]*                   |
| **cantidad_mov**              | 5.00                       | *[nulo]*                   | *[nulo]*                   |
| **fecha_movimiento**          | 2026-06-24                 | *[nulo]*                   | *[nulo]*                   |
| **motivo_movimiento**         | Consumo dia                | *[nulo]*                   | *[nulo]*                   |

**Anomalías identificadas en 0FN:**
- **Anomalía de Inserción:** No es posible registrar un nuevo cargo (con su sueldo base) si no hay al menos un empleado contratado para asignarle dicho rol. Tampoco se puede registrar un insumo en el sistema si este no está asignado todavía a la receta de un producto.
- **Anomalía de Borrado:** Si se despide al único empleado con el cargo "Administrador" (empleado_id 1), se perderá el registro del cargo y de su sueldo base.
- **Anomalía de Modificación:** Si una sucursal cambia de dirección o teléfono, se deben actualizar múltiples registros correspondientes a todos los empleados y movimientos asociados, con el riesgo de generar inconsistencias.

---

### 1.2 Primera Forma Normal (1FN) — Eliminación de grupos repetitivos

Para cumplir la 1FN, se separan los grupos repetitivos en tablas independientes con registros atómicos identificados por claves primarias (PK):

#### Tabla: SUCURSAL_1FN
| **sucursal_id** (PK) | nombre             | direccion           | ciudad | telefono  |
| :------------------- | :----------------- | :------------------ | :----- | :-------- |
| 1                    | Sede Miraflores    | Av. Larco 123       | Lima   | 014567890 |
| 2                    | Sede San Borja     | Av. Aviación 456    | Lima   | 014567891 |

#### Tabla: EMPLEADO_1FN
| **empleado_id** (PK) | nombres | apellidos | dni      | nombre_cargo  | sueldo_base | sucursal_id (FK) | fecha_contratacion | estado |
| :------------------- | :------ | :-------- | :------- | :------------ | :---------- | :--------------- | :----------------- | :----- |
| 1                    | Luis    | Ramirez   | 70001122 | Administrador | 3500.00     | 1                | 2024-03-01         | ACTIVO |
| 2                    | Maria   | Castillo  | 70002233 | Cajero        | 1500.00     | 1                | 2024-05-15         | ACTIVO |
| 3                    | Jorge   | Salas     | 70003344 | Cocinero      | 1800.00     | 1                | 2024-06-01         | ACTIVO |
| 4                    | Paola   | Reyes     | 70004455 | Cajero        | 1500.00     | 2                | 2024-07-10         | ACTIVO |

#### Tabla: HORARIO_1FN
| **horario_id** (PK) | empleado_id (FK) | dia_semana | hora_inicio | hora_fin |
| :------------------ | :--------------- | :--------- | :---------- | :------- |
| 1                   | 1                | Lunes      | 08:00       | 17:00    |
| 2                   | 1                | Martes     | 08:00       | 17:00    |
| 3                   | 2                | Lunes      | 09:00       | 18:00    |

#### Tabla: ASISTENCIA_1FN
| **asistencia_id** (PK) | empleado_id (FK) | fecha      | hora_entrada | hora_salida | observacion |
| :--------------------- | :--------------- | :--------- | :----------- | :---------- | :---------- |
| 101                    | 1                | 2026-07-01 | 07:55        | 17:05       | PUNTUAL     |

#### Tabla: PRODUCTO_1FN
| **producto_id** (PK) | nombre_producto | nombre_categoria | descripcion_cat           | precio | disponible |
| :------------------- | :-------------- | :--------------- | :------------------------ | :----- | :--------- |
| 1                    | Causa Limeña    | Entrada          | Platos de entrada         | 18.00  | S          |
| 2                    | Lomo Saltado    | Fondo            | Platos de fondo           | 25.50  | S          |
| 3                    | Chicha Morada   | Bebida           | Bebidas frias y calientes | 12.00  | S          |
| 4                    | Aji de Gallina  | Fondo            | Platos de fondo           | 35.00  | S          |

#### Tabla: INSUMO_1FN
| **insumo_id** (PK) | nombre_insumo | unidad_medida | stock_actual | stock_minimo |
| :----------------- | :------------ | :------------ | :----------- | :----------- |
| 1                  | Papa          | kg            | 50.00        | 10.00        |
| 2                  | Carne de Res  | kg            | 30.00        | 5.00         |
| 3                  | Maiz Morado   | kg            | 15.00        | 3.00         |

#### Tabla: PRODUCTO_INSUMO_1FN
| **producto_id** (PK, FK) | **insumo_id** (PK, FK) | cantidad_requerida | nombre_producto | nombre_insumo |
| :----------------------- | :--------------------- | :----------------- | :-------------- | :------------ |
| 1                        | 1                      | 0.30               | Causa Limeña    | Papa          |
| 2                        | 2                      | 0.25               | Lomo Saltado    | Carne de Res  |
| 3                        | 3                      | 0.10               | Chicha Morada   | Maiz Morado   |

> *Nota:* La estructura de `PRODUCTO_INSUMO_1FN` viola la 2FN porque los atributos no clave (`nombre_producto` y `nombre_insumo`) no dependen de la clave primaria completa `(producto_id, insumo_id)`, sino de subconjuntos de la misma.

#### Tabla: MOVIMIENTO_INVENTARIO_1FN
| **movimiento_id** (PK) | insumo_id (FK) | sucursal_id (FK) | tipo_movimiento | cantidad | fecha_movimiento | motivo            |
| :--------------------- | :------------- | :--------------- | :-------------- | :------- | :--------------- | :---------------- |
| 1                      | 1              | 1                | SALIDA          | 5.00     | 2026-06-24       | Consumo dia 24/06 |

---

### 1.3 Segunda Forma Normal (2FN) — Eliminación de dependencias parciales

**Regla:** Toda relación en 2FN debe estar en 1FN y no poseer dependencias parciales (todos los atributos no clave deben depender de la clave primaria completa).

- **Claves simples:** Las relaciones `SUCURSAL_1FN`, `EMPLEADO_1FN`, `HORARIO_1FN`, `ASISTENCIA_1FN`, `PRODUCTO_1FN`, `INSUMO_1FN` y `MOVIMIENTO_INVENTARIO_1FN` poseen claves primarias de un solo atributo. Por lo tanto, satisfacen la 2FN directamente.
- **Claves compuestas:** En la relación `PRODUCTO_INSUMO_1FN`, con clave primaria compuesta `(producto_id, insumo_id)`, se eliminan las dependencias parciales removiendo los atributos que no dependen de ambos componentes de la clave:
  - `nombre_producto` depende de `producto_id`.
  - `nombre_insumo` depende de `insumo_id`.
  
Al trasladar estos atributos a sus respectivas tablas maestras (`PRODUCTO_2FN` e `INSUMO_2FN`), la tabla resultante cumple la 2FN:

#### Tabla: PRODUCTO_INSUMO_2FN
| **producto_id** (PK, FK) | **insumo_id** (PK, FK) | cantidad_requerida |
| :----------------------- | :--------------------- | :----------------- |
| 1                        | 1                      | 0.30               |
| 2                        | 2                      | 0.25               |
| 3                        | 3                      | 0.10               |

---

### 1.4 Tercera Forma Normal (3FN) — Eliminación de dependencias transitivas

**Regla:** La relación debe estar en 2FN y no presentar dependencias transitivas (ningún atributo no clave puede depender de otro atributo no clave).

#### Análisis de dependencias transitivas resueltas:
1. **En la entidad Empleados:** Existe la dependencia transitiva:
   $$\text{empleado\_id} \longrightarrow \text{nombre\_cargo} \longrightarrow \text{sueldo\_base}$$
   *Resolución:* Se extrae la tabla **CARGO** y se asocia mediante `cargo_id` (FK) en **EMPLEADO**.
   
2. **En la entidad Productos:** Existe la dependencia transitiva:
   $$\text{producto\_id} \longrightarrow \text{nombre\_categoria} \longrightarrow \text{descripcion\_cat}$$
   *Resolución:* Se extrae la tabla **CATEGORIA** y se asocia mediante `categoria_id` (FK) en **PRODUCTO**.

#### Modelo Físico Final en 3FN (Oracle):

#### Tabla: SUCURSAL
| sucursal_id (PK) | nombre           | direccion        | ciudad | telefono  |
| :--------------- | :--------------- | :--------------- | :----- | :-------- |
| 1                | Sede Miraflores  | Av. Larco 123    | Lima   | 014567890 |
| 2                | Sede San Borja   | Av. Aviación 456 | Lima   | 014567891 |

#### Tabla: CARGO
| cargo_id (PK) | nombre_cargo  | sueldo_base |
| :------------ | :------------ | :---------- |
| 1             | Administrador | 3500.00     |
| 2             | Supervisor    | 2800.00     |
| 3             | Cajero        | 1500.00     |
| 4             | Cocinero      | 1800.00     |

#### Tabla: EMPLEADO
| empleado_id (PK) | nombres | apellidos | dni (UNIQUE) | cargo_id (FK) | sucursal_id (FK) | fecha_contratacion | estado |
| :--------------- | :------ | :-------- | :----------- | :------------ | :--------------- | :----------------- | :----- |
| 1                | Luis    | Ramirez   | 70001122     | 1             | 1                | 2024-03-01         | ACTIVO |
| 2                | Maria   | Castillo  | 70002233     | 3             | 1                | 2024-05-15         | ACTIVO |
| 3                | Jorge   | Salas     | 70003344     | 4             | 1                | 2024-06-01         | ACTIVO |
| 4                | Paola   | Reyes     | 70004455     | 3             | 2                | 2024-07-10         | ACTIVO |

#### Tabla: HORARIO
| horario_id (PK) | empleado_id (FK) | dia_semana | hora_inicio | hora_fin |
| :-------------- | :--------------- | :--------- | :---------- | :------- |
| 1               | 1                | Lunes      | 08:00       | 17:00    |
| 2               | 2                | Lunes      | 09:00       | 18:00    |

#### Tabla: ASISTENCIA
| asistencia_id (PK) | empleado_id (FK) | fecha      | hora_entrada | hora_salida | observacion |
| :----------------- | :--------------- | :--------- | :----------- | :---------- | :---------- |
| 101                | 1                | 2026-07-01 | 07:55        | 17:05       | PUNTUAL     |

#### Tabla: CATEGORIA
| categoria_id (PK) | nombre_categoria (UNIQUE) | descripcion               |
| :---------------- | :------------------------ | :------------------------ |
| 1                 | Entrada                   | Platos de entrada         |
| 2                 | Fondo                     | Platos de fondo           |
| 3                 | Bebida                    | Bebidas frias y calientes |

#### Tabla: PRODUCTO
| producto_id (PK) | nombre_producto | categoria_id (FK) | precio | disponible |
| :--------------- | :-------------- | :---------------- | :----- | :--------- |
| 1                | Causa Limeña    | 1                 | 18.00  | S          |
| 2                | Lomo Saltado    | 2                 | 25.50  | S          |
| 3                | Chicha Morada   | 3                 | 12.00  | S          |
| 4                | Aji de Gallina  | 2                 | 35.00  | S          |

#### Tabla: INSUMO
| insumo_id (PK) | nombre_insumo | unidad_medida | stock_actual | stock_minimo |
| :------------- | :------------ | :------------ | :----------- | :----------- |
| 1              | Papa          | kg            | 50.00        | 10.00        |
| 2              | Carne de Res  | kg            | 30.00        | 5.00         |
| 3              | Maiz Morado   | kg            | 15.00        | 3.00         |

#### Tabla: PRODUCTO_INSUMO
| producto_id (PK, FK) | insumo_id (PK, FK) | cantidad_requerida |
| :------------------- | :----------------- | :----------------- |
| 1                    | 1                  | 0.30               |
| 2                    | 2                  | 0.25               |
| 3                    | 3                  | 0.10               |

#### Tabla: MOVIMIENTO_INVENTARIO
| movimiento_id (PK) | insumo_id (FK) | sucursal_id (FK) | tipo_movimiento | cantidad | fecha_movimiento | motivo            |
| :----------------- | :------------- | :--------------- | :-------------- | :------- | :--------------- | :---------------- |
| 1                  | 1              | 1                | SALIDA          | 5.00     | 2026-06-24       | Consumo dia 24/06 |

---

## 2. Dominio Front-Office — PostgreSQL

### 2.1 Relación Universal (0FN — Sin normalizar)

En el dominio transaccional, modelamos una relación plana universal que consolida ventas, reservas y fidelización.

#### Tabla 2.1: Relación Universal PostgreSQL (0FN Transpuesta)

| Atributo (Columna)            | Registro de Ejemplo 1      | Registro de Ejemplo 2      | Registro de Ejemplo 3      |
| :---------------------------- | :------------------------- | :------------------------- | :------------------------- |
| **cliente_id**                | 1                          | 1                          | 2                          |
| **nombres_cli**               | Gianella                   | Gianella                   | Renzo                      |
| **apellidos_cli**             | Torres                     | Torres                     | Quispe                     |
| **email**                     | gianella.torres@mail.com   | gianella.torres@mail.com   | renzo.quispe@mail.com      |
| **telefono_cli**              | 987654321                  | 987654321                  | 912345678                  |
| **fecha_registro**            | 2026-07-01                 | 2026-07-01                 | 2026-07-01                 |
| **reserva_id**                | 201                        | *[nulo]*                   | 202                        |
| **fecha_reserva**             | 2026-07-01                 | *[nulo]*                   | 2026-07-02                 |
| **hora_reserva**              | 19:30                      | *[nulo]*                   | 20:00                      |
| **num_personas**              | 4                          | *[nulo]*                   | 2                          |
| **estado_reserva**            | CONFIRMADA                 | *[nulo]*                   | CONFIRMADA                 |
| **pedido_id**                 | 1                          | 1                          | 2                          |
| **empleado_id**               | 3                          | 3                          | 4                          |
| **sucursal_id**               | 1                          | 1                          | 2                          |
| **tipo_pedido**               | SALON                      | SALON                      | DELIVERY                   |
| **fecha_pedido**              | 2026-07-01                 | 2026-07-01                 | 2026-07-01                 |
| **estado_pedido**             | PENDIENTE                  | PENDIENTE                  | PENDIENTE                  |
| **direccion_delivery**        | *[nulo]*                   | *[nulo]*                   | Av. ejemplo                |
| **producto_id**               | 10                         | 11                         | 12                         |
| **nombre_producto**           | Causa Limeña               | Chicha Morada              | Aji de Gallina             |
| **cantidad**                  | 2                          | 1                          | 1                          |
| **precio_unitario**           | 25.50                      | 12.00                      | 35.00                      |
| **subtotal**                  | 51.00                      | 12.00                      | 35.00                      |
| **nombre_metodo_pago**        | Tarjeta                    | Tarjeta                    | Yape                       |
| **monto_pago**                | 63.00                      | 63.00                      | 35.00                      |
| **fecha_pago**                | 2026-07-01                 | 2026-07-01                 | 2026-07-01                 |
| **numero_comprobante**        | F001-000123                | F001-000123                | F001-000124                |
| **tipo_fidelizacion**         | ACUMULACION                | ACUMULACION                | ACUMULACION                |
| **puntos**                    | 6                          | 6                          | 3                          |

---

### 2.2 Primera Forma Normal (1FN) — Eliminación de grupos repetitivos

Descomponemos los grupos repetitivos en tablas con claves primarias independientes:

#### Tabla: CLIENTE_1FN
| **cliente_id** (PK) | nombres  | apellidos | email                    | telefono  | fecha_registro |
| :------------------ | :------- | :-------- | :----------------------- | :-------- | :------------- |
| 1                   | Gianella | Torres    | gianella.torres@mail.com | 987654321 | 2026-07-01     |
| 2                   | Renzo    | Quispe    | renzo.quispe@mail.com    | 912345678 | 2026-07-01     |

#### Tabla: RESERVA_1FN
| **reserva_id** (PK) | cliente_id (FK) | sucursal_id | fecha_reserva | hora_reserva | num_personas | estado     |
| :------------------ | :-------------- | :---------- | :------------ | :----------- | :----------- | :--------- |
| 201                 | 1               | 1           | 2026-07-01    | 19:30        | 4            | CONFIRMADA |
| 202                 | 2               | 2           | 2026-07-02    | 20:00        | 2            | CONFIRMADA |

#### Tabla: PEDIDO_1FN
| **pedido_id** (PK) | cliente_id (FK) | empleado_id | sucursal_id | tipo_pedido | fecha_pedido | estado_pedido | direccion_delivery | nombre_metodo_pago | monto_pago | fecha_pago | numero_comprobante |
| :----------------- | :-------------- | :---------- | :---------- | :---------- | :----------- | :------------ | :----------------- | :----------------- | :--------- | :--------- | :----------------- |
| 1                  | 1               | 3           | 1           | SALON       | 2026-07-01   | PENDIENTE     | *[nulo]*           | Tarjeta            | 63.00      | 2026-07-01 | F001-000123        |
| 2                  | 2               | 4           | 2           | DELIVERY    | 2026-07-01   | PENDIENTE     | Av. ejemplo        | Yape               | 35.00      | 2026-07-01 | F001-000124        |

#### Tabla: DETALLE_PEDIDO_1FN
| **detalle_id** (PK) | pedido_id (FK) | producto_id | nombre_producto | cantidad | precio_unitario | subtotal |
| :------------------ | :------------- | :---------- | :-------------- | :------- | :-------------- | :------- |
| 1                   | 1              | 10          | Causa Limeña    | 2        | 25.50           | 51.00    |
| 2                   | 1              | 11          | Chicha Morada   | 1        | 12.00           | 12.00    |
| 3                   | 2              | 12          | Aji de Gallina  | 1        | 35.00           | 35.00    |

#### Tabla: FIDELIZACION_MOVIMIENTO_1FN
| **movimiento_id** (PK) | cliente_id (FK) | tipo         | puntos | fecha      | pedido_id (FK) |
| :--------------------- | :-------------- | :----------- | :----- | :--------- | :------------- |
| 1                      | 1               | ACUMULACION  | 6      | 2026-07-01 | 1              |
| 2                      | 2               | ACUMULACION  | 3      | 2026-07-01 | 2              |

---

### 2.3 Segunda Forma Normal (2FN) — Eliminación de dependencias parciales

Dado que todas las tablas resultantes en 1FN (`CLIENTE_1FN`, `RESERVA_1FN`, `PEDIDO_1FN`, `DETALLE_PEDIDO_1FN` y `FIDELIZACION_MOVIMIENTO_1FN`) poseen llaves primarias simples basadas en un único atributo, satisfacen la 2FN directamente al no existir la posibilidad de dependencias parciales sobre subclaves.

---

### 2.4 Tercera Forma Normal (3FN) — Eliminación de dependencias transitivas

#### Análisis de dependencias transitivas resueltas:

1. **La entidad Pago en Pedido:**
   En la tabla `PEDIDO_1FN`, la información del pago (`monto_pago`, `fecha_pago`, `numero_comprobante`) y el `nombre_metodo_pago` dependen transitivamente de la transacción de compra. Adicionalmente, el nombre del método de pago depende de un identificador de método de pago.
   - *Corrección:* Se extrae la entidad **PAGO** con una relación 1:1 referenciando a `pedido_id` (para evitar registrar múltiples pagos de un mismo pedido) y se crea la tabla catálogo **METODO_PAGO**.

2. **Información Redundante Inter-Motores (Modelo Distribuido):**
   En `DETALLE_PEDIDO_1FN`, el atributo `nombre_producto` depende funcionalmente de `producto_id`, que es una entidad externa administrada en Oracle. Almacenar el nombre en PostgreSQL genera redundancia inter-motor.
   - *Corrección:* Se retira `nombre_producto` de la tabla de detalles. La aplicación Swing realiza la lectura cruzada utilizando `producto_id` como enlace lógico.

3. **Columna calculada `subtotal`:**
   El subtotal es un valor derivado matemáticamente de $cantidad \times precio\_unitario$. En lugar de almacenarse de manera redundante, en el modelo de PostgreSQL se define como una columna virtual generada y persistida:
   `subtotal NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED`.

#### Modelo Físico Final en 3FN (PostgreSQL):

#### Tabla: CLIENTE
| cliente_id (PK) | nombres  | apellidos | email (UNIQUE)           | telefono  | fecha_registro |
| :-------------- | :------- | :-------- | :----------------------- | :-------- | :------------- |
| 1               | Gianella | Torres    | gianella.torres@mail.com | 987654321 | 2026-07-01     |
| 2               | Renzo    | Quispe    | renzo.quispe@mail.com    | 912345678 | 2026-07-01     |

#### Tabla: METODO_PAGO
| metodo_pago_id (PK) | nombre_metodo (UNIQUE) |
| :------------------ | :--------------------- |
| 1                   | Efectivo               |
| 2                   | Tarjeta                |
| 3                   | Yape                   |
| 4                   | Plin                   |
| 5                   | Transferencia          |

#### Tabla: RESERVA
| reserva_id (PK) | cliente_id (FK) | sucursal_id (FK lógica) | fecha_reserva | hora_reserva | num_personas | estado     |
| :-------------- | :-------------- | :---------------------- | :------------ | :----------- | :----------- | :--------- |
| 201             | 1               | 1                       | 2026-07-01    | 19:30        | 4            | CONFIRMADA |
| 202             | 2               | 2                       | 2026-07-02    | 20:00        | 2            | CONFIRMADA |

#### Tabla: PEDIDO
| pedido_id (PK) | cliente_id (FK) | empleado_id (FK lógica) | sucursal_id (FK lógica) | tipo_pedido | fecha_pedido | estado_pedido | direccion_delivery |
| :------------- | :-------------- | :---------------------- | :---------------------- | :---------- | :----------- | :------------ | :----------------- |
| 1              | 1               | 3                       | 1                       | SALON       | 2026-07-01   | PENDIENTE     | *[nulo]*           |
| 2              | 2               | 4                       | 2                       | DELIVERY    | 2026-07-01   | PENDIENTE     | Av. ejemplo        |

#### Tabla: DETALLE_PEDIDO
| detalle_id (PK) | pedido_id (FK) | producto_id (FK lógica) | cantidad | precio_unitario | subtotal (GENERADA) |
| :-------------- | :------------- | :---------------------- | :------- | :-------------- | :------------------ |
| 1               | 1              | 10                      | 2        | 25.50           | 51.00               |
| 2               | 1              | 11                      | 1        | 12.00           | 12.00               |
| 3               | 2              | 12                      | 1        | 35.00           | 35.00               |

> *Nota:* `precio_unitario` se almacena en `DETALLE_PEDIDO` como un **snapshot histórico** para asegurar la trazabilidad transaccional. Esto protege el registro histórico de ventas ante futuras actualizaciones en los precios del catálogo en Oracle.

#### Tabla: PAGO
| pago_id (PK) | pedido_id (FK, UNIQUE) | metodo_pago_id (FK) | monto | fecha_pago | numero_comprobante (UNIQUE) |
| :----------- | :--------------------- | :------------------ | :---- | :--------- | :-------------------------- |
| 1            | 1                      | 2                   | 63.00 | 2026-07-01 | F001-000123                 |
| 2            | 2                      | 3                   | 35.00 | 2026-07-01 | F001-000124                 |

#### Tabla: FIDELIZACION_MOVIMIENTO
| movimiento_id (PK) | cliente_id (FK) | tipo        | puntos | fecha      | pedido_id (FK) |
| :----------------- | :-------------- | :---------- | :----- | :--------- | :------------- |
| 1                  | 1               | ACUMULACION | 6      | 2026-07-01 | 1              |
| 2                  | 2               | ACUMULACION | 3      | 2026-07-01 | 2              |

---

## 3. Resumen del Proceso de Normalización

### 3.1 Transformaciones aplicadas

| Paso       | Forma Normal           | Qué se corrigió                                                                                                                   | Tablas afectadas          |
| :--------- | :--------------------- | :-------------------------------------------------------------------------------------------------------------------------------- | :------------------------ |
| 0FN → 1FN  | Primera Forma Normal   | Se eliminaron grupos repetitivos (horarios, asistencia de empleado, insumos de producto, detalles de pedido, movimientos y reservas). | Todo el esquema           |
| 1FN → 2FN  | Segunda Forma Normal   | Eliminación de dependencias parciales en claves compuestas. Se corrigió `PRODUCTO_INSUMO` separando los nombres descriptivos.     | PRODUCTO_INSUMO           |
| 2FN → 3FN  | Tercera Forma Normal   | Se extrajeron las entidades CARGO y CATEGORIA en Oracle; y PAGO, METODO_PAGO en PostgreSQL. Se quitó `nombre_producto` de detalles. | EMPLEADO, PRODUCTO, PEDIDO, DETALLE_PEDIDO |

### 3.2 Resultado final — Inventario de tablas en 3FN

| N.° | Tabla                   | Motor      | Clave Primaria       | Claves Foráneas / Dependencias Cruzadas                                 |
| :-- | :---------------------- | :--------- | :------------------- | :---------------------------------------------------------------------- |
| 1   | SUCURSAL                | Oracle     | sucursal_id          | —                                                                       |
| 2   | CARGO                   | Oracle     | cargo_id             | —                                                                       |
| 3   | EMPLEADO                | Oracle     | empleado_id          | cargo_id → CARGO, sucursal_id → SUCURSAL                                |
| 4   | HORARIO                 | Oracle     | horario_id           | empleado_id → EMPLEADO                                                  |
| 5   | ASISTENCIA              | Oracle     | asistencia_id        | empleado_id → EMPLEADO                                                  |
| 6   | CATEGORIA               | Oracle     | categoria_id         | —                                                                       |
| 7   | PRODUCTO                | Oracle     | producto_id          | categoria_id → CATEGORIA                                                |
| 8   | INSUMO                  | Oracle     | insumo_id            | —                                                                       |
| 9   | PRODUCTO_INSUMO         | Oracle     | (prod_id, insumo_id) | producto_id → PRODUCTO, insumo_id → INSUMO                              |
| 10  | MOVIMIENTO_INVENTARIO   | Oracle     | movimiento_id        | insumo_id → INSUMO, sucursal_id → SUCURSAL                              |
| 11  | CLIENTE                 | PostgreSQL | cliente_id           | —                                                                       |
| 12  | METODO_PAGO             | PostgreSQL | metodo_pago_id       | —                                                                       |
| 13  | RESERVA                 | PostgreSQL | reserva_id           | cliente_id → CLIENTE, sucursal_id → Oracle.SUCURSAL (Lógica)            |
| 14  | PEDIDO                  | PostgreSQL | pedido_id            | cliente_id → CLIENTE, empleado_id → Oracle.EMPLEADO (Lógica), sucursal_id → Oracle.SUCURSAL (Lógica) |
| 15  | DETALLE_PEDIDO          | PostgreSQL | detalle_id           | pedido_id → PEDIDO, producto_id → Oracle.PRODUCTO (Lógica)              |
| 16  | PAGO                    | PostgreSQL | pago_id              | pedido_id → PEDIDO (UNIQUE), metodo_pago_id → METODO_PAGO               |
| 17  | FIDELIZACION_MOVIMIENTO | PostgreSQL | movimiento_id        | cliente_id → CLIENTE, pedido_id → PEDIDO                                |

### 3.3 Dependencias funcionales verificadas en 3FN

**Notación:** A → B significa "A determina funcionalmente a B".

```
SUCURSAL:           sucursal_id → {nombre, direccion, ciudad, telefono}
CARGO:              cargo_id → {nombre_cargo, sueldo_base}
EMPLEADO:           empleado_id → {nombres, apellidos, dni, cargo_id, sucursal_id, fecha_contratacion, estado}
HORARIO:            horario_id → {empleado_id, dia_semana, hora_inicio, hora_fin}
ASISTENCIA:         asistencia_id → {empleado_id, fecha, hora_entrada, hora_salida, observacion}
CATEGORIA:          categoria_id → {nombre_categoria, descripcion}
PRODUCTO:           producto_id → {nombre_producto, categoria_id, precio, disponible}
INSUMO:             insumo_id → {nombre_insumo, unidad_medida, stock_actual, stock_minimo}
PRODUCTO_INSUMO:    (producto_id, insumo_id) → {cantidad_requerida}
MOVIMIENTO_INV:     movimiento_id → {insumo_id, sucursal_id, tipo_movimiento, cantidad, fecha_movimiento, motivo}
CLIENTE:            cliente_id → {nombres, apellidos, email, telefono, fecha_registro}
METODO_PAGO:        metodo_pago_id → {nombre_metodo}
RESERVA:            reserva_id → {cliente_id, sucursal_id, fecha_reserva, hora_reserva, num_personas, estado}
PEDIDO:             pedido_id → {cliente_id, empleado_id, sucursal_id, tipo_pedido, fecha_pedido, estado_pedido, direccion_delivery}
DETALLE_PEDIDO:     detalle_id → {pedido_id, producto_id, cantidad, precio_unitario}
PAGO:               pago_id → {pedido_id, metodo_pago_id, monto, fecha_pago, numero_comprobante}
FIDELIZACION_MOV:   movimiento_id → {cliente_id, tipo, puntos, fecha, pedido_id}
```

Todas las dependencias funcionales corresponden a claves que determinan directamente a sus atributos no clave. El diseño físico final de la base de datos cumple estrictamente con las condiciones de la Tercera Forma Normal (3FN).
