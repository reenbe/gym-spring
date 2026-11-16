package app.web;

import app.domain.InvalidDataException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class QueryParams {

    private QueryParams() {
    }

    static void allowOnly(Map<String, String> params, String... allowed) {
        List<String> names = Arrays.asList(allowed);
        for (String name : params.keySet()) {
            if (!names.contains(name)) {
                throw new InvalidDataException("неизвестный параметр фильтра: " + name);
            }
        }
    }

    static Boolean bool(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null) {
            return null;
        }
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        throw new InvalidDataException(name + " должен быть true или false");
    }

    static <E extends Enum<E>> E enumValue(Map<String, String> params, String name, Class<E> type) {
        String value = params.get(name);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(name + " должен быть одним из: " + Arrays.toString(type.getEnumConstants()));
        }
    }
}
