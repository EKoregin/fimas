package ru.korevg.fimas.dto.address;

import ru.korevg.fimas.dto.RecordWithName;

public record AddressShortResponse(
        Long id,
        String name
) implements RecordWithName {}
