# Artefacto 3 — Entorno NoSQL (MongoDB) y Big Data (Hadoop)

## 1. Colección `pedidos_operativos` (documento embebido, listo para front de cocina/delivery)

```json
{
  "_id": "PED-2026-0001",
  "pedido_id_relacional": 1,
  "sucursal": {
    "id": 1,
    "nombre": "Sede Miraflores"
  },
  "cliente": {
    "id": 1,
    "nombre": "Gianella Torres"
  },
  "tipo_pedido": "SALON",
  "estado": "ENTREGADO",
  "fecha": "2026-06-24T19:45:00Z",
  "items": [
    { "producto_id": 10, "nombre": "Causa Limeña", "cantidad": 2, "precio_unitario": 25.50 },
    { "producto_id": 11, "nombre": "Chicha Morada", "cantidad": 1, "precio_unitario": 12.00 }
  ],
  "total": 63.00
}
```

## 2. Colección `recetas` (estructura de receta para cocina, copia documental de PRODUCTO_INSUMO)

```json
{
  "_id": "REC-002",
  "producto_id": 2,
  "nombre_producto": "Lomo Saltado",
  "categoria": "Fondo",
  "tiempo_preparacion_min": 20,
  "ingredientes": [
    { "insumo": "Carne de Res", "cantidad": 0.25, "unidad": "kg" },
    { "insumo": "Cebolla", "cantidad": 0.1, "unidad": "kg" },
    { "insumo": "Tomate", "cantidad": 0.1, "unidad": "kg" }
  ],
  "pasos": [
    "Saltear la carne a fuego alto",
    "Agregar cebolla y tomate",
    "Servir con papas fritas y arroz"
  ]
}
```

## 3. Comandos de inserción

```javascript
use restaurante_nosql

db.pedidos_operativos.insertOne({
  _id: "PED-2026-0002",
  pedido_id_relacional: 2,
  sucursal: { id: 2, nombre: "Sede San Borja" },
  cliente: { id: 2, nombre: "Renzo Quispe" },
  tipo_pedido: "DELIVERY",
  estado: "PREPARACION",
  fecha: new Date(),
  items: [
    { producto_id: 12, nombre: "Aji de Gallina", cantidad: 1, precio_unitario: 35.00 }
  ],
  total: 35.00
});

db.recetas.insertOne({
  _id: "REC-001",
  producto_id: 1,
  nombre_producto: "Causa Limeña",
  categoria: "Entrada",
  tiempo_preparacion_min: 15,
  ingredientes: [
    { insumo: "Papa", cantidad: 0.3, unidad: "kg" },
    { insumo: "Palta", cantidad: 0.2, unidad: "kg" }
  ],
  pasos: ["Cocer y prensar la papa", "Rellenar con palta y pollo", "Refrigerar antes de servir"]
});
```

## 4. Comandos de consulta

```javascript
// Pedidos pendientes por sucursal (vista operativa para cocina)
db.pedidos_operativos.find({ "sucursal.id": 1, estado: { $ne: "ENTREGADO" } });

// Ventas totales agrupadas por sucursal (equivalente NoSQL de un reporte SQL)
db.pedidos_operativos.aggregate([
  { $group: { _id: "$sucursal.nombre", total_ventas: { $sum: "$total" }, num_pedidos: { $sum: 1 } } },
  { $sort: { total_ventas: -1 } }
]);

// Producto más vendido (desempaquetando el array items)
db.pedidos_operativos.aggregate([
  { $unwind: "$items" },
  { $group: { _id: "$items.nombre", cantidad_total: { $sum: "$items.cantidad" } } },
  { $sort: { cantidad_total: -1 } },
  { $limit: 5 }
]);

// Receta de un producto específico
db.recetas.findOne({ producto_id: 2 });
```

---

## 5. Big Data — Carga de ventas históricas a Hadoop HDFS

### 5.1 Preparar el archivo de ventas históricas (export desde PostgreSQL)

```bash
# Desde PostgreSQL, exportar la vista vw_ventas_pedido a CSV
psql -U postgres -d restaurante_frontoffice -c \
  "\copy (SELECT * FROM vw_ventas_pedido) TO 'ventas_historicas.csv' WITH CSV HEADER"
```

### 5.2 Subir el archivo a HDFS

```bash
# Crear carpeta destino en HDFS
hdfs dfs -mkdir -p /restaurante/ventas

# Subir el CSV
hdfs dfs -put ventas_historicas.csv /restaurante/ventas/

# Verificar
hdfs dfs -ls /restaurante/ventas
hdfs dfs -cat /restaurante/ventas/ventas_historicas.csv | head -5
```

### 5.3 Crear tabla externa en Hive sobre el archivo en HDFS

```sql
CREATE EXTERNAL TABLE ventas_hist (
    pedido_id     INT,
    sucursal_id   INT,
    fecha_pedido  TIMESTAMP,
    monto         DECIMAL(10,2)
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/restaurante/ventas/'
TBLPROPERTIES ("skip.header.line.count"="1");
```

### 5.4 Métricas de productos/sucursales más vendidos (HiveQL → MapReduce internamente)

```sql
-- Total vendido por sucursal (procesado en MapReduce por debajo de Hive)
SELECT sucursal_id, SUM(monto) AS total_vendido, COUNT(*) AS num_pedidos
FROM ventas_hist
GROUP BY sucursal_id
ORDER BY total_vendido DESC;

-- Tendencia de ventas por mes (demanda histórica)
SELECT sucursal_id, DATE_FORMAT(fecha_pedido, 'yyyy-MM') AS mes, SUM(monto) AS total_mes
FROM ventas_hist
GROUP BY sucursal_id, DATE_FORMAT(fecha_pedido, 'yyyy-MM')
ORDER BY mes;
```

> Para tu informe: captura el `hdfs dfs -ls`, el resultado del `CREATE EXTERNAL TABLE` y el
> resultado de cualquiera de las dos consultas (el job de MapReduce se ve en consola con las
> líneas `Stage-1 map = 100%, reduce = 100%`) — esa captura es justamente la evidencia de
> "procesamiento Big Data" que exige la Fase 5.

### 5.5 Si tu profesor pide MapReduce puro (Java) en vez de Hive

Lógica equivalente en pseudocódigo MapReduce, por si te lo piden explícito en código:

```
Mapper:   por cada línea del CSV -> emitir (sucursal_id, monto)
Reducer:  por cada sucursal_id -> sumar todos los montos recibidos -> emitir (sucursal_id, total)
```

Esto es exactamente lo que Hive genera automáticamente al ejecutar el `GROUP BY` de arriba —
puedes mencionarlo en la sustentación para demostrar que entiendes lo que pasa "debajo" de Hive.


// ============ 6. BACKUP Y RESTORE (MongoDB Atlas, ejecutar desde CMD) ============

// NOTA: Estos comandos se ejecutan en la terminal (CMD) de Windows.
// Primero debes navegar a la carpeta de MongoDB Tools si tu Windows no la reconoce.

// 1. Entrar a la carpeta de herramientas de Mongo:
// cd "C:\Program Files\MongoDB\Tools\100\bin"

// 2. Backup (exportar todo el clúster a la carpeta Documentos):
// mongodump --uri="mongodb+srv://admin_nosql:[TU_PASSWORD]@<TU_CLUSTER>.mongodb.net/" --out="backup_mongo"

// 3. Restore (restaurar la información desde la carpeta hacia la nube):
// mongorestore --uri="mongodb+srv://admin_nosql:[TU_PASSWORD]@<TU_CLUSTER>.mongodb.net/" --dir="backup_mongo"

// TIP para el Word: Ejecutar el mongodump en el CMD y tomar captura a la consola
// cuando confirme que se exportaron las colecciones ("done dumping").

