package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter UI_DATE =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final DateTimeFormatter UI_DATETIME =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    // For LocalDate (Lost/Found dates)
    public static String format(LocalDate date) {
        return date == null ? "—" : date.format(UI_DATE);
    }

    // For LocalDateTime (created_at / updated_at)
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.format(UI_DATETIME);
    }
}