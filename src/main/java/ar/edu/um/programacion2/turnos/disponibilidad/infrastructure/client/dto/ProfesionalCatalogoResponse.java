package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto;

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
public class ProfesionalCatalogoResponse {
    private Long id;
    private Long categoriaId;
    private String nombre;
    private String apellido;
    private boolean habilitado;
    private List<HorarioCatalogoResponse> horarios;
    private Long versionCatalogo;
}
