package app.web.dto;

import app.domain.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberRequest(
        @NotNull(message = "поле type обязательно: REGULAR, VIP, SILVER или GOLD") MemberType type,
        @NotBlank(message = "имя клиента не может быть пустым") String name,
        @NotNull(message = "поле age обязательно") Integer age) {
}
