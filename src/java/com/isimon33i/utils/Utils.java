package com.isimon33i.utils;

import java.util.List;

public class Utils {

    private Utils() {
    }

    public static List<String> filterByStart(String startsWith, List<String> input, boolean caseSensitive) {
        if (caseSensitive) {
            return input.stream().filter(x -> x.startsWith(startsWith)).toList();
        } else {
            var startsWithLowerCase = startsWith.toLowerCase();
            return input.stream().filter(x -> x.toLowerCase().startsWith(startsWithLowerCase)).toList();
        }
    }

    public record SplitedTime(int days, int hours, int minutes, int seconds) {

    }

    public static SplitedTime splitMillis(long millis) {
        long totalSeconds = millis / 1000;

        int days = (int) (totalSeconds / (24 * 3600));
        int hours = (int) ((totalSeconds % (24 * 3600)) / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);

        return new SplitedTime(days, hours, minutes, seconds);
    }
}
