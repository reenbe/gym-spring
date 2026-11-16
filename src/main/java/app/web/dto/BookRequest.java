package app.web.dto;

import jakarta.validation.constraints.NotNull;

public record BookRequest(
        @NotNull(message = "поле memberId обязательно") Integer memberId) {
}
