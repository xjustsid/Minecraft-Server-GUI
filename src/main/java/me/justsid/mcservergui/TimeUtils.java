package me.justsid.mcservergui;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

public class TimeUtils {

    /**
     * Formatiert eine Zeitdauer in Sekunden als hh:mm:ss.
     *
     * @param seconds Die Zeitdauer in Sekunden.
     * @return Die formatierte Zeit als String im Format hh:mm:ss.
     */
    public static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Berechnet die nächste Zeit aus einer Liste von Zeiten basierend auf der aktuellen Zeit.
     *
     * @param times Ein Array von Zeiten als Strings im Format HH:mm.
     * @param now   Die aktuelle Zeit.
     * @return Die nächste geplante Zeit als LocalDateTime.
     */
    public static LocalDateTime getNextTime(String[] times, LocalDateTime now) {
        if (times == null || times.length == 0) {
            throw new IllegalArgumentException("Die Zeiten dürfen nicht null oder leer sein.");
        }
        if (now == null) {
            throw new IllegalArgumentException("Die aktuelle Zeit darf nicht null sein.");
        }
        LocalDateTime finalNow = now;
        return Arrays.stream(times)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(LocalTime::parse)
                .map(localTime -> finalNow.with(localTime))
                .filter(candidate -> candidate.isAfter(finalNow))
                .findFirst()
                .orElseGet(() -> {
                    String firstTime = times[0].trim();
                    return finalNow.plusDays(1).with(LocalTime.parse(firstTime));
                });
    }
}
