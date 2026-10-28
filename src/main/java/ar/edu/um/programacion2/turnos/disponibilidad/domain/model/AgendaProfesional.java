package ar.edu.um.programacion2.turnos.disponibilidad.domain.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaProfesional {
    private Long profesionalId;
    private Long categoriaId;
    private String nombre;
    private String apellido;
    private boolean habilitado;
    private List<HorarioAtencion> horarios;
    private Long versionCatalogo;
}
