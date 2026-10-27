package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.um.programacion2.turnos.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.UsuarioNotFoundException;
import ar.edu.um.programacion2.turnos.usuario.application.service.UsuarioService;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.LoginRequest;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.TokenResponse;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioRequest;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioResponse;
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

    @PostMapping("/authenticate")
    public ResponseEntity<Object> authenticateUsuario(@Valid @RequestBody LoginRequest request) {
        try {
            String token = usuarioService.authenticateUsuario(request.getUsername(), request.getPassword());
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            return new ResponseEntity<>(new TokenResponse(token), headers, HttpStatus.OK);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage()));
        }
    }

    @GetMapping("/account")
    public ResponseEntity<UsuarioResponse> getAccount(@AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(usuarioDtoMapper.toResponse(usuarioService.getUsuarioByLogin(jwt.getSubject())));
        } catch (UsuarioNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
