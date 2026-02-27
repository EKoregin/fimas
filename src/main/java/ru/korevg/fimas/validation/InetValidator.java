package ru.korevg.fimas.validation;

import org.springframework.stereotype.Service;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Сервис для валидации IP-адресов и CIDR-префиксов (IPv4 и IPv6).
 */
@Service
public class InetValidator {

    // Регулярное выражение для базовой проверки формата
    private static final Pattern IPV4_CIDR_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                    "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:/(3[0-2]|[0-9]|[12][0-9]))?$"
    );

    private static final Pattern IPV6_CIDR_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}(?:/\\d{1,3})?$|" +
                    "^([0-9a-fA-F]{1,4}:){1,7}:[0-9a-fA-F]{0,4}(?:/\\d{1,3})?$|" +
                    "^([0-9a-fA-F]{1,4}:){1,6}:([0-9a-fA-F]{1,4}:)?[0-9a-fA-F]{0,4}(?:/\\d{1,3})?$|" +
                    "^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}(?:/\\d{1,3})?$|" +
                    "^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}(?:/\\d{1,3})?$|" +
                    "^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}(?:/\\d{1,3})?$|" +
                    "^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}(?:/\\d{1,3})?$|" +
                    "^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})(?:/\\d{1,3})?$|" +
                    "^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{0,4}(?:/\\d{1,3})?$"
    );

    /**
     * Проверяет, является ли строка корректным IP-адресом или CIDR-префиксом (IPv4 или IPv6).
     *
     * @param value строка для проверки (например: "192.168.1.1", "10.0.0.0/16", "2001:db8::/32")
     * @return true — если строка является валидным IP или CIDR, false — в противном случае
     */
    public boolean isValidInet(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String trimmed = value.trim();

        // 1. Проверка через регулярное выражение (быстрый фильтр)
        if (trimmed.contains(".")) {
            if (!IPV4_CIDR_PATTERN.matcher(trimmed).matches()) {
                return false;
            }
        } else if (trimmed.contains(":")) {
            if (!IPV6_CIDR_PATTERN.matcher(trimmed).matches()) {
                return false;
            }
        } else {
            return false;
        }

        // 2. Более строгая проверка через InetAddress
        try {
            if (trimmed.contains("/")) {
                // CIDR-нотация
                String[] parts = trimmed.split("/", 2);
                if (parts.length != 2) {
                    return false;
                }

                String ipPart = parts[0];
                int prefixLength = Integer.parseInt(parts[1]);

                InetAddress addr = InetAddress.getByName(ipPart);

                // Проверка длины префикса
                if (addr instanceof Inet6Address) {
                    return prefixLength >= 0 && prefixLength <= 128;
                } else {
                    return prefixLength >= 0 && prefixLength <= 32;
                }
            } else {
                // Одиночный IP-адрес
                InetAddress.getByName(trimmed);
                return true;
            }
        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * Проверяет строку и бросает исключение при ошибке.
     *
     * @param value строка для проверки
     * @throws IllegalArgumentException если строка не является валидным IP/CIDR
     */
    public void validateInetOrThrow(String value) {
        if (!isValidInet(value)) {
            throw new IllegalArgumentException(
                    "Некорректный формат IP-адреса или подсети: '" + value + "'. " +
                            "Ожидается IPv4 (например 192.168.1.1), IPv6 или CIDR (например 10.0.0.0/16, 2001:db8::/32)"
            );
        }
    }
}
