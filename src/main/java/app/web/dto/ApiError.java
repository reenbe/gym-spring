package app.web.dto;

/** Единый формат ошибки: {"error": "..."}. */
public record ApiError(String error) {
}
