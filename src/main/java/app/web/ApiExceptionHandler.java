package app.web;

import app.domain.InvalidDataException;
import app.domain.NotFoundException;
import app.domain.RuleViolationException;
import app.web.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;


@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ApiError> invalidData(InvalidDataException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RuleViolationException.class)
    public ResponseEntity<ApiError> ruleViolation(RuleViolationException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message.isEmpty() ? "некорректные данные запроса" : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "неверный JSON, неверные типы полей или пустое тело запроса");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e) {
        if (e.getParameter().hasParameterAnnotation(PathVariable.class)) {
            return error(HttpStatus.NOT_FOUND, "ресурс не найден");
        }
        return error(HttpStatus.BAD_REQUEST, "неверное значение параметра " + e.getName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            return ResponseEntity.status(status)
                    .headers(er.getHeaders())
                    .body(new ApiError(messageFor(status, er)));
        }
        log.error("Необработанная ошибка", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "внутренняя ошибка сервера");
    }

    private static String messageFor(HttpStatusCode status, org.springframework.web.ErrorResponse er) {
        return switch (status.value()) {
            case 404 -> "путь не найден";
            case 405 -> "метод не поддерживается";
            case 415 -> "неподдерживаемый тип содержимого, ожидался application/json";
            default -> {
                String detail = er.getBody().getDetail();
                yield detail != null ? detail : "некорректный запрос";
            }
        };
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(message));
    }
}
