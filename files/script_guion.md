# Guion de Sustentación — Sistema Integral de Gestión para una Cadena de Restaurantes

**Reparto:**
- 🟠 **RODOLFO** → Introducción, Modelo de datos, Normalización y todo el bloque **SQL relacional (PostgreSQL + Oracle)**
- 🟢 **GIANFRANCO** → **NoSQL (MongoDB)**, **Big Data (Hadoop/Hive)**, Aplicación Java Swing y cierre

Duración estimada total: **10-12 minutos** (≈35-40 seg por diapositiva)

## Diapositiva 11 — Seguridad y roles

**GIANFRANCO:**
"Antes de entrar a NoSQL, quiero mostrarles cómo protegimos toda esta información. Configuramos cuatro roles tanto en PostgreSQL como en Oracle: el Administrador tiene acceso completo; el Supervisor solo puede consultar información y reportes; el Cajero registra y consulta clientes, pedidos y pagos; y el Cocinero únicamente consulta productos e inventario y actualiza el estado de los pedidos. Todo esto sigue el principio de mínimo privilegio, y la aplicación Java valida el rol del usuario apenas inicia sesión."

---

## Diapositiva 12 — Backup y Restore

**GIANFRANCO:**
"También implementamos procedimientos de respaldo para cada motor: en PostgreSQL usamos pg_dump y pg_restore; en Oracle, las utilidades Data Pump, expdp e impdp; y en MongoDB usamos mongodump apuntando directamente a nuestro clúster en Atlas. Esto nos permite recuperar la información completa ante cualquier falla o pérdida de datos."

---

## Diapositiva 13 — MongoDB (NoSQL)

**GIANFRANCO:**
"Pasando a la parte NoSQL: usamos MongoDB para almacenar información que necesita una estructura más flexible. Creamos dos colecciones principales. La primera, `pedidos_operativos`, guarda cada pedido junto con sus productos embebidos directamente en el mismo documento, evitando tener que hacer JOINs como en el modelo relacional. La segunda, `recetas`, guarda cada producto con sus ingredientes y cantidades. Además, usamos el framework de Aggregation de MongoDB para calcular ventas totales por sucursal y el producto más vendido. En la imagen se ve nuestra pantalla de consulta, donde se puede ver el detalle completo de un pedido, incluyendo el documento JSON embebido."

---

## Diapositiva 14 — Big Data: Hadoop y Hive

**GIANFRANCO:**
"Para el análisis de grandes volúmenes de datos, montamos un entorno de Big Data con Hadoop y Hive usando contenedores Docker. El flujo funciona así: primero exportamos el histórico de ventas de PostgreSQL a un archivo CSV; luego lo subimos al sistema de archivos distribuido HDFS con el comando `hdfs dfs -put`; después creamos una tabla externa en Hive apuntando a ese archivo; y finalmente ejecutamos consultas en HiveQL para obtener indicadores como ventas totales por sucursal o productos más vendidos. En la imagen se ve uno de nuestros reportes ya generados, con su gráfico comparativo de ventas."

---

## Diapositiva 15 — Aplicación Java Swing

**GIANFRANCO:**
"Todo lo que les hemos mostrado se integra en una sola aplicación de escritorio hecha en Java Swing. Primero está la pantalla de inicio de sesión, que valida las credenciales y habilita funciones según el rol. Luego el panel principal, donde se puede ver en vivo el estado de conexión de los cuatro motores: PostgreSQL, Oracle, MongoDB y Hive. Desde ahí se accede a los módulos de pedidos, inventario, consultas NoSQL y reportes analíticos. Por ejemplo, cuando se registra un pedido, la aplicación valida primero contra Oracle que el empleado, producto y sucursal existan, y solo después lo guarda en PostgreSQL."

---

## Diapositiva 16 — Conclusiones

**RODOLFO:**
"Como conclusión, logramos integrar exitosamente PostgreSQL, Oracle, MongoDB y Hadoop/Hive en una sola aplicación, aprovechando la fortaleza específica de cada motor."

**GIANFRANCO:**
"La implementación de procedimientos, funciones, vistas y validaciones nos permitió mantener la integridad de los datos y automatizar procesos clave, incluso sin llaves foráneas físicas entre motores distintos."

**RODOLFO / GIANFRANCO (juntos):**
"Muchas gracias por su atención. Quedamos atentos a sus preguntas."

---

### 💡 Tips rápidos para la sustentación
- Practiquen la transición entre la diapositiva 10 y 11: es el punto donde Rodolfo le cede la palabra a Gianfranco.
- Si les preguntan por qué no usaron llaves foráneas físicas entre motores, la respuesta corta es: "porque son proveedores distintos; la validación cruzada la hace la aplicación Java."
- Tengan a la mano el detalle de un procedimiento SQL (Rodolfo) y una consulta de Aggregation en MongoDB (Gianfranco) por si piden ver código en vivo.