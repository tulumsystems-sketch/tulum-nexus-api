package com.tulumcore.api.services;

import com.tulumcore.api.entities.Producto;
import com.tulumcore.api.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {
   @Autowired
   private ProductoRepository productoRepository;

   public List<Producto> getAllProductos(){
       return productoRepository.findAll();
   }
   public Optional<Producto> getProductoById(Long id){
       return productoRepository.findById(id);
   }
   public Producto createOrUpdateProducto(Producto producto){
       return productoRepository.save(producto);
   }
   public void deleteProducto(Long id){
       productoRepository.deleteById(id);
   }

   public void adjustStock(Long id, int cantidad){
       Producto producto = productoRepository.findById(id).orElseThrow(() -> new RuntimeException("Producto no econtrado"));
       producto.setCantidadStock(producto.getCantidadStock() + cantidad);
       productoRepository.save(producto);
   }
    public List<Producto> getLatestProductos(int limit) {
        return productoRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Order.desc("id")))).getContent();
    }
}
