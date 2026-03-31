package ru.korevg.fimas.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.korevg.fimas.entity.AddressSubType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class InetValidator {

    // === Регулярки для базовой проверки ===
    private static final Pattern IPV4_CIDR_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                    "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:/(3[0-2]|[0-9]|[12][0-9]))?$"
    );

    private static final Pattern IPV4_RANGE_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                    "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +
                    "\\s*-\\s*" +
                    "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                    "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
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
            "^(?=.{1,253}$)" +
                    "(?:\\*\\.)?" +                             // wildcard опционально
                    "(?:" +
                    "(?!-)[a-z0-9-]{1,63}(?<!-)" +       // лейбл
                    "\\." +
                    ")+" +
                    // TLD: 2–63 символа, буквы/цифры/дефис, не начинается/не заканчивается на -
                    "(?!-)[a-z0-9-]{2,63}(?<!-)$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Основной метод: проверяет IP, CIDR, IP-Range или FQDN.
     */
    public boolean isValidInet(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String trimmed = value.trim();

        // 1. Проверка диапазона
        if (trimmed.contains("-")) {
            return isValidIPRange(trimmed);
        }

        // 2. Проверка IPv4 / CIDR
        if (trimmed.contains(".")) {
            return IPV4_CIDR_PATTERN.matcher(trimmed).matches() && isValidSingleInet(trimmed);
        }

        // 3. Проверка IPv6 / CIDR
        if (trimmed.contains(":")) {
            return IPV6_CIDR_PATTERN.matcher(trimmed).matches() && isValidSingleInet(trimmed);
        }

        return false;
    }

    /**
     * Проверка одиночного IP или CIDR (без диапазона)
     */
    private boolean isValidSingleInet(String value) {
        try {
            if (value.contains("/")) {
                String[] parts = value.split("/", 2);
                String ipPart = parts[0];
                int prefix = Integer.parseInt(parts[1]);

                InetAddress addr = InetAddress.getByName(ipPart);
                if (addr.getAddress().length == 16) { // IPv6
                    return prefix >= 0 && prefix <= 128;
                } else {
                    return prefix >= 0 && prefix <= 32;
                }
            } else {
                InetAddress.getByName(value);
                return true;
            }
        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * Новая проверка диапазона IPv4 вида "start-ip - end-ip"
     */
    public boolean isValidIPRange(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String trimmed = value.trim();

        // Быстрая проверка формата через regex
        if (!IPV4_RANGE_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        try {
            String[] parts = trimmed.split("\\s*-\\s*", 2);
            if (parts.length != 2) return false;

            String startStr = parts[0].trim();
            String endStr = parts[1].trim();

            // Проверяем, что оба конца — валидные IPv4
            InetAddress start = InetAddress.getByName(startStr);
            InetAddress end = InetAddress.getByName(endStr);

            if (start.getAddress().length != 4 || end.getAddress().length != 4) {
                return false; // не IPv4
            }

            // Сравниваем численно (big-endian)
            long startLong = ipToLong(startStr);
            long endLong = ipToLong(endStr);

            return startLong <= endLong;

        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * Преобразует IPv4 в long для сравнения
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Integer.parseInt(octets[i]) & 0xFF);
        }
        return result;
    }

    /**
     * Бросает исключение, если значение некорректно
     */
    public void validateInetOrThrow(String value) {
        if (!isValidInet(value)) {
            throw new IllegalArgumentException(
                    "Некорректный формат адреса: '" + value + "'. " +
                            "Поддерживаются: IPv4, CIDR (10.0.0.0/16), диапазон (11.99.0.0-11.99.99.255), IPv6."
            );
        }
    }

    /**
     * Валидация набора адресов с учётом типа (IP / FQDN)
     */
    public void validateAddresses(Set<String> addresses, AddressSubType subType) {
        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalArgumentException("Адреса не могут быть пустыми");
        }
        if (subType == null) {
            throw new IllegalArgumentException("subType должен быть указан");
        }

        Set<String> normalizedAddresses = new HashSet<>();
        for (String addr : addresses) {
            if (addr == null) continue;
            String normalized = addr.trim();
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
                    // Теперь сюда попадают и одиночные IP/CIDR, и диапазоны
                    validateInetOrThrow(normalized);
                    break;

                case FQDN:
                    if (!isValidFqdn(normalized)) {
                        throw new IllegalArgumentException("Некорректный домен для типа FQDN: " + normalized);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Неизвестный тип адреса: " + subType);
            }
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
}
