package br.edu.banco.api;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TratamentoErros {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> recurso(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode())
                .body(Map.of("mensagem", String.valueOf(erro.getReason())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> regra(IllegalArgumentException erro) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", erro.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> entrada(Exception erro) {
        return ResponseEntity.badRequest().body(Map.of("mensagem",
                "Confira os campos: IDs positivos, valorCentavos inteiro positivo e tipo valido"));
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<?> limite(ArithmeticException erro) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Saldo acima do limite suportado"));
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<?> conflito(Exception erro) {
        return ResponseEntity.status(409).body(Map.of("mensagem",
                "Conflito de gravacao; consulte o historico antes de repetir"));
    }
}
