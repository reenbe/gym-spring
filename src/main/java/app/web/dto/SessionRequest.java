package app.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SessionRequest(
        @NotBlank(message = "название занятия не может быть пустым") String name,
        @NotNull(message = "поле capacity обязательно") Integer capacity) {
}
