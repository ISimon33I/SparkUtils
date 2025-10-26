package com.isimon33i.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class SunCalc {

    public enum SunStatus {
        NORMAL, // The sun rises and sets normally
        MIDNIGHT_SUN, // The sun never sets
        POLAR_NIGHT    // The sun never rises
    }

    public static class SunTimes {

        public SunStatus status;
        public LocalTime sunrise;
        public LocalTime sunset;

        public SunTimes(SunStatus status, LocalTime sunrise, LocalTime sunset) {
            this.status = status;
            this.sunrise = sunrise;
            this.sunset = sunset;
        }

        @Override
        public String toString() {
            return switch (status) {
                case POLAR_NIGHT ->
                    "Polar night – the sun does not rise on this day.";
                case MIDNIGHT_SUN ->
                    "Midnight sun – the sun does not set on this day.";
                default ->
                    String.format("Sunrise: %s, Sunset: %s", sunrise, sunset);
            };
        }
    }

    /**
     * Calculates sunrise and sunset times, or determines whether it is a polar
     * night or midnight sun.
     */
    public static SunTimes calculateSunriseSunset(LocalDate date, double latitude, double longitude, ZoneId timeZone) {
        int dayOfYear = date.getDayOfYear();
        double decl = 23.45 * Math.sin(Math.toRadians((360.0 / 365.0) * (284 + dayOfYear)));

        double cosH = -Math.tan(Math.toRadians(latitude)) * Math.tan(Math.toRadians(decl));

        if (cosH > 1) {
            // Polar night
            return new SunTimes(SunStatus.POLAR_NIGHT, null, null);
        } else if (cosH < -1) {
            // Midnight sun
            return new SunTimes(SunStatus.MIDNIGHT_SUN, null, null);
        }

        // Normal day
        double H = Math.toDegrees(Math.acos(cosH));
        double hours = H / 15.0;

        double sunriseSolar = 12 - hours;
        double sunsetSolar = 12 + hours;

        ZoneOffset offset = timeZone.getRules().getOffset(date.atStartOfDay());
        double timeZoneLongitude = offset.getTotalSeconds() / 3600.0 * 15.0;
        double correction = (timeZoneLongitude - longitude) / 15.0;

        double sunriseLocal = sunriseSolar + correction;
        double sunsetLocal = sunsetSolar + correction;

        LocalTime sunrise = convertToTime(sunriseLocal);
        LocalTime sunset = convertToTime(sunsetLocal);

        return new SunTimes(SunStatus.NORMAL, sunrise, sunset);
    }

    private static LocalTime convertToTime(double decimalHours) {
        int hour = (int) decimalHours;
        int minute = (int) ((decimalHours - hour) * 60);
        return LocalTime.of((hour + 24) % 24, minute);
    }

    /**
     * Maps real local time to Minecraft time (0–24000 ticks) based on sunrise
     * and sunset times.
     *
     * @param localTime The current local time (e.g. LocalTime.now())
     * @param sunrise The local time of sunrise
     * @param sunset The local time of sunset
     * @return Minecraft time in ticks (0–24000)
     */
    public static int mapToMinecraftTime(LocalTime localTime, LocalTime sunrise, LocalTime sunset) {
        double sunriseSec = sunrise.toSecondOfDay();
        double sunsetSec = sunset.toSecondOfDay();
        double currentSec = localTime.toSecondOfDay();

        // Minecraft day cycle:
        // 0      = sunrise
        // 12000  = sunset
        // 24000  = next sunrise
        double dayLengthSec = (sunsetSec - sunriseSec);
        if (dayLengthSec < 0) {
            dayLengthSec += 86400; // Handle crossing over midnight
        }
        double nightLengthSec = 86400 - dayLengthSec;

        double ticks;
        if (isBetween(currentSec, sunriseSec, sunsetSec)) {
            // Daytime: map to 0–12000
            double progress = (currentSec - sunriseSec);
            if (progress < 0) {
                progress += 86400;
            }
            ticks = 0 + (progress / dayLengthSec) * 12000;
        } else {
            // Nighttime: map to 12000–24000
            double progress = (currentSec - sunsetSec);
            if (progress < 0) {
                progress += 86400;
            }
            ticks = 12000 + (progress / nightLengthSec) * 12000;
        }

        return (int) Math.round(ticks % 24000);
    }

    private static boolean isBetween(double current, double start, double end) {
        if (start < end) {
            return current >= start && current < end;
        } else {
            return current >= start || current < end; // across midnight
        }
    }

}
