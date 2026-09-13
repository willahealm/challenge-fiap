package br.com.fiap.embarque;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    final HttpStatus status;
    final String code;
    public ApiException(HttpStatus status, String code, String message) { super(message); this.status=status; this.code=code; }
    static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","Acesso não permitido."); }
    static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","Recurso não encontrado."); }
    static ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_REQUEST",message); }
}
