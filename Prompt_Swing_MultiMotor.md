# PROMPT — Generación de Sistema Java Swing Multi-Motor (Restaurante)

Copia y pega todo el bloque de abajo (desde "ROL Y CONTEXTO" hasta el final) en la IA que
vayas a usar para generar el código (Antigravity, Claude Code, etc.). Está armado para que
la IA no tenga que adivinar nada de tu arquitectura ya definida.

---

## ROL Y CONTEXTO

Actúa como un Desarrollador Senior Java Full-Stack especializado en aplicaciones de
escritorio con Swing y sistemas de bases de datos distribuidas. Voy a describirte un
proyecto universitario ya diseñado y con las bases de datos ya implementadas y con datos
de prueba cargadas. Tu tarea es generar el código Java completo de un sistema de escritorio
(JFrame) que conecte y opere sobre 4 fuentes de datos simultáneamente: PostgreSQL, Oracle,
MongoDB y Hive (sobre Hadoop).

No me expliques teoría de bases de datos ni me preguntes si quiero que uses JDBC — ya sé
qué es y ya tengo los motores funcionando. Ve directo a generar código Java compilable,
organizado por paquetes, con manejo de errores real (try-catch, mensajes claros al usuario
vía JOptionPane) y comentarios en español explicando qué hace cada método.

## CONTEXTO DEL PROYECTO

Es un "Sistema Integral de Gestión para una Cadena de Restaurantes", proyecto final del
curso de Base de Datos II. La arquitectura ya decidida divide los datos así:

- **Oracle** (dominio back-office): sucursal, cargo, empleado, horario, asistencia,
  categoria, producto, insumo, producto_insumo, movimiento_inventario
- **PostgreSQL** (dominio front-office): cliente, metodo_pago, reserva, pedido,
  detalle_pedido, pago, fidelizacion_movimiento
- **MongoDB** (colecciones documentales): `pedidos_operativos` (pedido con items embebidos)
  y `recetas` (receta con ingredientes embebidos)
- **Hive sobre Hadoop** (corriendo en Docker, puerto 10000): tabla externa `ventas_hist`
  para métricas de ventas por sucursal, cargada desde HDFS

Como un motor no puede tener FK física hacia otro motor, las referencias cruzadas
(`pedido.empleado_id` → Oracle.empleado, `pedido.sucursal_id` → Oracle.sucursal,
`detalle_pedido.producto_id` → Oracle.producto) se validan a nivel de aplicación Java
ANTES de insertar — este patrón de "validación cruzada" ya está parcialmente implementado
y debe respetarse y expandirse, no rediseñarse.

## RÚBRICA QUE EL SOFTWARE DEBE CUMPLIR

El proyecto se evalúa así (debes asegurarte de que el software final sea evidencia de
cada punto, ya que se sustenta en vivo):

1. Modelo entidad-relación (ya resuelto, no aplica al código)
2. Implementación SQL PostgreSQL/Oracle (ya resuelto, tablas y datos ya existen)
3. **Seguridad y administración**: el software debe soportar login con roles
   (admin, cajero, cocinero, supervisor) y restringir pantallas/acciones según el rol
4. **NoSQL aplicado**: el software debe tener una pantalla que lea y muestre datos desde
   MongoDB (pedidos operativos y/o recetas), no solo desde SQL
5. **Big Data**: el software debe tener una pantalla de "Reportes/Métricas" que consulte
   Hive (vía Hive JDBC) y muestre resultados de ventas por sucursal
6. Sustentación (no aplica al código, pero el software debe ser demostrable en vivo sin
   errores frente al profesor)

## CONEXIONES A CONFIGURAR (usar como constantes o archivo de config externo)

```
PostgreSQL (Neon):   jdbc:postgresql://<host-neon>/restaurante_frontoffice?sslmode=require
Oracle (Cloud Autonomous DB): jdbc:oracle:thin:@<tns-alias>?TNS_ADMIN=<ruta-wallet>
MongoDB (Atlas):     mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/restaurante_nosql
Hive (Docker local):  jdbc:hive2://localhost:10000/default
```

Genera una clase `ConfiguracionConexiones.java` centralizada que lea estos valores (usa
constantes con placeholders claros tipo `TU_HOST_AQUI` para que yo los reemplace, NO
inventes credenciales reales) — no hardcodees usuario/password en cada clase DAO.

## DRIVERS A USAR

- PostgreSQL: `org.postgresql:postgresql` (JDBC estándar)
- Oracle: `com.oracle.database.jdbc:ojdbc11` (JDBC estándar, requiere wallet para cloud)
- MongoDB: `org.mongodb:mongodb-driver-sync` (driver nativo, NO es JDBC)
- Hive: `org.apache.hive:hive-jdbc` (JDBC)

## ESTRUCTURA DE PAQUETES ESPERADA

```
com.restaurante.conexion   -> clases de conexión a cada motor (una por motor + config central)
com.restaurante.dao        -> clases DAO (una por entidad principal: EmpleadoDAO,
                               ProductoDAO, PedidoDAO, ClienteDAO, InventarioDAO,
                               PedidoMongoDAO, VentasHiveDAO)
com.restaurante.modelo      -> clases POJO (Empleado, Producto, Pedido, DetallePedido,
                               Cliente, Usuario/Rol, etc.)
com.restaurante.vista       -> las pantallas JFrame
com.restaurante.util        -> validaciones cruzadas, utilidades de formato, manejo de
                               excepciones común
com.restaurante.Main        -> clase con el método main() que lanza el login
```

## PANTALLAS (JFrame) A GENERAR

1. **LoginFrame**: usuario/contraseña + selector o detección automática de rol
   (admin, cajero, cocinero, supervisor). Al autenticar, abre el MenuPrincipalFrame
   mostrando solo los botones/opciones permitidas para ese rol.

2. **MenuPrincipalFrame**: menú con botones hacia las demás pantallas, habilitados/
   deshabilitados según el rol logueado.

3. **RegistroPedidoFrame**: formulario para registrar un pedido nuevo — selecciona
   cliente (combo desde Postgres), empleado y sucursal (combo desde Oracle), agrega
   productos (combo desde Oracle) con cantidad, calcula el total, y al guardar ejecuta
   la validación cruzada contra Oracle ANTES de insertar en Postgres (reutiliza y expande
   la lógica de `validarReferenciasOracle` / `registrarPedidoConValidacionCruzada` ya
   existente). Debe mostrar mensaje de error claro si la validación cruzada falla.

4. **InventarioFrame**: tabla (JTable) mostrando insumos desde Oracle con su stock actual
   y stock mínimo, resaltando en rojo (o con ícono) los que están por debajo del mínimo.
   Botón para registrar una salida de insumo (llama al procedimiento
   `sp_registrar_salida_insumo` ya existente en Oracle).

5. **PedidosMongoFrame**: tabla (JTable) que lista los documentos de la colección
   `pedidos_operativos` desde MongoDB (pedido, sucursal, estado, total), con un
   detalle expandible o un panel lateral mostrando los items embebidos del pedido
   seleccionado.

6. **ReportesHiveFrame**: pantalla con un botón "Generar reporte de ventas por
   sucursal" que ejecuta la query Hive (`SELECT sucursal_id, SUM(monto)... GROUP BY`)
   y muestra el resultado en una JTable o, si es posible, en un gráfico simple
   (JFreeChart si está disponible, si no, tabla es suficiente).

7. **GestionRolesFrame** (solo visible para admin): pantalla simple que liste los
   4 roles y una descripción de qué permisos tiene cada uno (puede ser informativo,
   no necesita CRUD real de roles ya que estos ya están creados en las bases de datos).

## REQUISITOS NO FUNCIONALES

- Todo el código con manejo de excepciones (try-catch específico por tipo de excepción:
  SQLException, MongoException, etc.), nunca dejar una excepción sin capturar que crashee
  la app — mostrar JOptionPane con mensaje entendible para el usuario final.
- Cerrar siempre las conexiones (usa try-with-resources donde aplique).
- Comentarios en español explicando el propósito de cada clase y método (no comentarios
  obvios tipo "// esto es un for", sino explicando el "por qué" cuando no sea evidente).
- Diseño Swing simple pero prolijo: usa `GridBagLayout` o `BorderLayout` con paneles,
  no dejes todo con el layout por defecto de NetBeans sin ajustar. No hace falta que sea
  bonito de diseño gráfico, pero sí que se vea ordenado y profesional para la sustentación.
- Indícame explícitamente, al final de tu respuesta, qué dependencias/jars debo agregar
  al proyecto NetBeans y en qué orden debo probar cada pantalla para verificar que
  funciona antes de la sustentación.

## FORMATO DE ENTREGA

Genera el código dividido por archivo (un bloque de código por clase, con el nombre de
archivo indicado antes de cada bloque), no todo en un solo bloque gigante. Empieza por
las clases de conexión y DAO, y termina con las pantallas Swing. Si algo requiere que yo
tome una decisión (por ejemplo cómo detectar el rol al hacer login), pregúntamelo ANTES
de generar esa parte en vez de asumir.
