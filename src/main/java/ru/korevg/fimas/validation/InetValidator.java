package ru.korevg.fimas.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.korevg.fimas.entity.AddressSubType;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Сервис для валидации IP-адресов и CIDR-префиксов (IPv4 и IPv6).
 */
@Slf4j
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

    private static final Pattern STRICT_FQDN_OR_WILDCARD_PATTERN = Pattern.compile(
            // Общая длина 1–253 символа (включая точки и *)
            "^(?=.{1,253}$)" +

                    // Опциональный wildcard в самом начале: либо ничего, либо "*."
                    "(?:\\*\\.)?" +

                    // Один или более лейблов (обычная часть домена)
                    "(?:" +
                    "(?!-)[a-z0-9-]{1,63}(?<!-)" +   // лейбл: не начинается и не заканчивается на -
                    "\\." +                          // точка-разделитель
                    ")+" +

                    // Финальный TLD (минимум 2 буквы, без цифр и дефисов в начале/конце)
                    "[a-z]{2,63}$",

            Pattern.CASE_INSENSITIVE
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

    public boolean isValidFqdn(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        if (!STRICT_FQDN_OR_WILDCARD_PATTERN.matcher(normalized).matches()) {
            return false;
        }

        String[] parts = normalized.split("\\.");

        if (parts.length < 2) {
            return false;
        }

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (i == 0 && "*".equals(part)) {
                continue;
            }

            if (part.isEmpty() || part.length() > 63) {
                return false;
            }

            if (part.startsWith("-") || part.endsWith("-")) {
                return false;
            }
        }

        return !normalized.contains("..") && !normalized.startsWith(".*");
    }

    public void validateAddresses(Set<String> addresses, AddressSubType subType) {
        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalArgumentException("Адреса не могут быть пустыми");
        }

        if (subType == null) {
            throw new IllegalArgumentException("subType должен быть указан");
        }

        Set<String> normalizedAddresses = new HashSet<>();
        for (String addr : addresses) {
            if (addr == null) continue;  // Пропускаем null
            String normalized = addr.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                normalizedAddresses.add(normalized);
            }
        }

        if (normalizedAddresses.isEmpty()) {
            throw new IllegalArgumentException("После нормализации адреса пусты");
        }

        for (String normalized : normalizedAddresses) {
            switch (subType) {
                case IP:
                    validateInetOrThrow(normalized);
                    break;

                case FQDN:
                    if (!isValidFqdn(normalized)) {
                        throw new IllegalArgumentException(
                                "Некорректный домен для типа FQDN: " + normalized);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Неизвестный тип адреса: " + subType);
            }
        }
    }
}
