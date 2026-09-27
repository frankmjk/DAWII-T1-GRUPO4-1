package com.cibertec.mstarjetas.repositorio;

import com.cibertec.mstarjetas.entidades.Tarjeta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarjetaRepository extends JpaRepository<Tarjeta, Long> {
}
