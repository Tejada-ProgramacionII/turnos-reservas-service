-- V5: cuándo quedó cancelada una reserva confirmada (CU-08), según la cátedra.
ALTER TABLE procesos_reserva ADD COLUMN cancelada_en TIMESTAMP WITH TIME ZONE;
