package ru.korevg.fimas.util;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileTool {

    public String readFileToString(String filePath) {
        String result = "";
        try {
            Path path = Path.of(filePath);
            result = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        return result;
    }
}
