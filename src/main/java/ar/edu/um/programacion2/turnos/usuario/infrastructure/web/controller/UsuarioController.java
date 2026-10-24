package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.programacion2.turnos.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.service.UsuarioService;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioRequest;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.mapper.UsuarioDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioDtoMapper usuarioDtoMapper;

    @PostMapping("/register")
    public ResponseEntity<ProblemDetail> registerUsuario(@Valid @RequestBody UsuarioRequest request) {
        try {
            usuarioService.createUsuario(usuarioDtoMapper.toDomain(request), request.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (LoginAlreadyUsedException | EmailAlreadyUsedException e) {
            return ResponseEntity.badRequest()
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }
}
