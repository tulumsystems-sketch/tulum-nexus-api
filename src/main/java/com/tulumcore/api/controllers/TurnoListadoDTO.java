package com.tulumcore.api.controllers;

import java.time.Instant;

public record TurnoListadoDTO(
        Long id,
        Instant inicio,
        Instant fin,
        int duracionMinutos,
        String estado,
        String modalidad,
        String notas,
        Long clienteId,
        String pacienteNombre,
        String dni,
        String telefono,
        String obraSocial,
        Long productoId,
        String practicaNombre,
        Double practicaPrecio,
        Long ventaId,
        boolean cobrado
) {}
