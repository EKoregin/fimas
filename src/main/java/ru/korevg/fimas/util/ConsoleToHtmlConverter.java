package ru.korevg.fimas.util;

import java.util.regex.Pattern;

public final class ConsoleToHtmlConverter {

    private ConsoleToHtmlConverter() {
        // утилитный класс
    }

    public static String convertToHtmlDiv(String consoleOutput) {
        if (consoleOutput == null || consoleOutput.isBlank()) {
            return "<div style='padding:15px; background:#1e1e1e; color:#d4d4d4; font-family:monospace;'>Вывод пустой</div>";
        }

        String escaped = escapeHtml(consoleOutput);

        // Заменяем переносы строк
        String html = escaped.replace("\r\n", "\n")
                .replace("\r", "\n");

        // Заменяем ведущие пробелы на &nbsp; (исправленный вариант)
        html = Pattern.compile("(?m)^( +)")
                .matcher(html)
                .replaceAll(mr -> "&nbsp;".repeat(mr.group(1).length()));

        // Основной HTML
        return """
                <div style="padding:16px; 
                            background-color:#1e1e1e; 
                            color:#d4d4d4; 
                            font-family:Consolas,Monaco,'Courier New',monospace; 
                            font-size:14px; 
                            line-height:1.5; 
                            white-space:pre-wrap; 
                            word-wrap:break-word; 
                            overflow-wrap:break-word; 
                            border:1px solid #444; 
                            border-radius:6px;">
                    %s
                </div>
                """.formatted(html);
    }

    /**
     * Экранирует специальные символы HTML
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
