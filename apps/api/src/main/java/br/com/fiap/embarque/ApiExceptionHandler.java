package br.com.fiap.embarque;

import java.time.Clock;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
class ApiExceptionHandler {
    private final Clock clock;
    ApiExceptionHandler(Clock clock) { this.clock=clock; }
    @ExceptionHandler(ApiException.class) ResponseEntity<Models.ApiError> business(ApiException ex) {
        return ResponseEntity.status(ex.status).body(new Models.ApiError(ex.code,ex.getMessage(),clock.instant()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    ResponseEntity<Models.ApiError> validation(Exception ex) {
        return ResponseEntity.badRequest().body(new Models.ApiError("INVALID_REQUEST","Confira os campos enviados.",clock.instant()));
    }
}
