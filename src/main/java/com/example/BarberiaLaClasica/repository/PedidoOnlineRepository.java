package com.example.BarberiaLaClasica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.PedidoOnline;

@Repository
public interface PedidoOnlineRepository extends JpaRepository<PedidoOnline, Long> {
    List<PedidoOnline> findByClienteOrderByFechaPedidoDesc(Cliente cliente);
    List<PedidoOnline> findAllByOrderByFechaPedidoDesc();
    List<PedidoOnline> findByEstadoOrderByFechaPedidoDesc(int estado);
}
