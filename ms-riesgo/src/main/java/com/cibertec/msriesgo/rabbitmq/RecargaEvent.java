package com.cibertec.msriesgo.rabbitmq;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Misma estructura JSON que publica ms-recargas.
public record RecargaEvent(
		Long idRecarga,
		Long idTarjeta,
		BigDecimal saldoDisponible,
		BigDecimal montoRecarga,
		LocalDateTime fechaRecarga
) {
}
