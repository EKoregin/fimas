package ru.korevg.fimas.validation;

/**
 * Утилитный класс для проверки корректности ввода портов (одиночный порт или диапазон).
 * Используется как в UI-валидации (Vaadin), так и в других местах, где нужна быстрая проверка без Jakarta Validation.
 */
public final class PortInputUtil {

    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;

    private PortInputUtil() {
        // утилитный класс — запрещаем создание экземпляров
    }

    /**
     * Проверяет, является ли строка корректным представлением порта или диапазона портов.
     * <p>
     * Допустимые форматы:
     * <ul>
     *   <li>одиночный порт: "80", "443", "0", "65535"</li>
     *   <li>диапазон: "1024-5000", "80-80", "1-65535"</li>
     * </ul>
     * Пустая строка / null считаются валидными (соответствует "любой порт").
     *
     * @param input строка, введённая пользователем (может быть null)
     * @return true — если формат корректен и значения в пределах 0–65535, start ≤ end
     */
    public static boolean isValidPortInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        String trimmed = input.trim();

        // Одиночный порт
        if (!trimmed.contains("-")) {
            return isValidPortNumber(trimmed);
        }

        // Диапазон start-end
        String[] parts = trimmed.split("-", 2);
        if (parts.length != 2) {
            return false;
        }

        String startStr = parts[0].trim();
        String endStr   = parts[1].trim();

        if (!isValidPortNumber(startStr) || !isValidPortNumber(endStr)) {
            return false;
        }

        try {
            int start = Integer.parseInt(startStr);
            int end   = Integer.parseInt(endStr);
            return start <= end;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Проверяет, является ли строка корректным номером порта (целое число 0–65535).
     *
     * @param s строка для проверки
     * @return true если это валидный порт
     */
    private static boolean isValidPortNumber(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        try {
            int port = Integer.parseInt(s);
            return port >= MIN_PORT && port <= MAX_PORT;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Вариант метода, который возвращает сообщение об ошибке вместо boolean.
     * Полезно для показа конкретной причины пользователю.
     *
     * @param input входная строка
     * @return null если валидно, иначе текст ошибки
     */
    public static String validatePortInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        if (!trimmed.contains("-")) {
            if (isValidPortNumber(trimmed)) {
                return null;
            }
            return getPortNumberErrorMessage(trimmed);
        }

        String[] parts = trimmed.split("-", 2);
        if (parts.length != 2) {
            return "Ожидается формат: число или start-end";
        }

        String startStr = parts[0].trim();
        String endStr   = parts[1].trim();

        if (!isValidPortNumber(startStr)) {
            return getPortNumberErrorMessage(startStr);
        }
        if (!isValidPortNumber(endStr)) {
            return getPortNumberErrorMessage(endStr);
        }

        try {
            int start = Integer.parseInt(startStr);
            int end   = Integer.parseInt(endStr);

            if (start > end) {
                return "Начальный порт не может быть больше конечного";
            }
            return null;
        } catch (NumberFormatException e) {
            return "Порты должны быть целыми числами";
        }
    }

    private static String getPortNumberErrorMessage(String s) {
        try {
            int port = Integer.parseInt(s);
            if (port < MIN_PORT || port > MAX_PORT) {
                return "Порт должен быть от 0 до 65535";
            }
            return "Ожидается целое число"; // на случай странных символов
        } catch (NumberFormatException e) {
            return "Ожидается целое число";
        }
    }
}
