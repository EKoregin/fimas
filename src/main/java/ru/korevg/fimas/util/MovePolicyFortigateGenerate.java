package ru.korevg.fimas.util;

import lombok.extern.slf4j.Slf4j;

public class MovePolicyFortigateGenerate {

    public static void main(String[] args) {
        System.out.println(buildMoveCommands(123, 280, 122));
    }

    private static String buildMoveCommands(int startId, int endId, int afterId) {
        StringBuilder sb = new StringBuilder();

        sb.append("config firewall policy\n");

        // Первая политика ставится after указанной
        sb.append("move ").append(startId).append(" after ").append(afterId).append("\n");

        // Остальные — по цепочке after предыдущей
        for (int i = startId + 1; i <= endId; i++) {
            sb.append("move ").append(i).append(" after ").append(i - 1).append("\n");
        }

        sb.append("end");

        return sb.toString();
    }
}
