package com.spiceroute.feedback.ai;

import java.util.regex.Pattern;

/**
 * Removes personal information from comment text before it is sent to the AI.
 * The original feedback row is never modified — this only operates on copies.
 *
 * Patterns covered:
 *   - Bangladeshi mobile numbers (01XXXXXXXXX, +8801XXXXXXXXX)
 *   - International phone formats (+X XXX XXX XXXX etc.)
 *   - Email addresses
 *   - "my name is …" / "I am …" self-identification phrases
 */
final class PiiStripper {

    private PiiStripper() {}

    private static final Pattern PHONE_BD =
            Pattern.compile("(?:\\+?880)?\\s*0?1[3-9]\\d{8}", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_INTL =
            Pattern.compile("\\+?\\d[\\d\\s()\\-]{7,}\\d");

    private static final Pattern EMAIL =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern NAME_PHRASE =
            Pattern.compile("(?:my name is|i am|i'm|ami)\\s+[A-Za-z\\u0980-\\u09FF]{2,}(?:\\s+[A-Za-z\\u0980-\\u09FF]{2,})?",
                    Pattern.CASE_INSENSITIVE);

    static String strip(String text) {
        if (text == null || text.isBlank()) return "";
        String result = text;
        result = PHONE_BD.matcher(result).replaceAll("[phone]");
        result = PHONE_INTL.matcher(result).replaceAll("[phone]");
        result = EMAIL.matcher(result).replaceAll("[email]");
        result = NAME_PHRASE.matcher(result).replaceAll("[name]");
        return result.trim();
    }
}
