package app.web.dto;

import app.domain.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Тело PUT /members/{id}. Поле type необязательное: тип менять нельзя, но можно прислать прежний. */
public record MemberUpdateRequest(
        MemberType type,
        @NotBlank(message = "имя клиента не может быть пустым") String name,
        @NotNull(message = "поле age обязательно") Integer age) {
}
