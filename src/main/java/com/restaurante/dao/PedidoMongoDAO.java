package com.restaurante.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.restaurante.conexion.ConexionManager;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones DAO para la base de datos documental MongoDB (Atlas).
 * Lee información de la colección de pedidos operativos.
 * 
 * @author Gianf
 */
public class PedidoMongoDAO {

    private static final String COLECCION_PEDIDOS = "pedidos_operativos";

    /**
     * Recupera todos los pedidos almacenados en MongoDB.
     */
    public List<Document> listarPedidosOperativos() throws Exception {
        List<Document> pedidos = new ArrayList<>();
        MongoDatabase db = ConexionManager.getInstancia().getBaseDatosMongo();
        
        if (db == null) {
            throw new Exception("MongoDB: Sin conexión activa.");
        }

        MongoCollection<Document> coleccion = db.getCollection(COLECCION_PEDIDOS);
        coleccion.find().forEach(pedidos::add);
        return pedidos;
    }
}
