package ru.korevg.fimas.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Converter(autoApply = true)   // автоматически применяется ко всем полям InetAddress
public class InetAddressConverter implements AttributeConverter<InetAddress, String> {

    @Override
    public String convertToDatabaseColumn(InetAddress attribute) {
        return attribute == null ? null : attribute.getHostAddress();
    }

    @Override
    public InetAddress convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return InetAddress.getByName(dbData);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + dbData, e);
        }
    }
}
