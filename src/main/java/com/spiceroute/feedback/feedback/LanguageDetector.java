package com.spiceroute.feedback.feedback;

import java.util.Set;

/**
 * Tags a comment as "bn" (Bangla script), "banglish" (Bangla written in Latin letters) or "en".
 * A lightweight heuristic: it is only a label for filtering and display, never used to hide feedback.
 */
public final class LanguageDetector {

    private LanguageDetector() {}

    private static final Set<String> BANGLISH_WORDS = Set.of(
            "khub", "khubi", "bhalo", "valo", "bhaloi", "valoi", "kharap", "kharab", "khabar", "khaboor",
            "shundor", "sundor", "shundar", "onek", "onk", "ekdom", "ekta", "thik", "theek", "beshi", "besi",
            "jonno", "jonne", "kore", "korechi", "korbo", "pelam", "peyechi", "dilo", "dite", "gelo", "gelam",
            "lage", "lagche", "lagse", "laglo", "mojar", "moja", "mazar", "mazza", "jhal", "jhaal", "shad",
            "swad", "shadu", "bhai", "vai", "apu", "bhaiya", "amar", "amader", "apnar", "tomar", "tader",
            "ekhane", "ekhanei", "abar", "aschi", "asbo", "ashbo", "ashchi", "hoyni", "hoyechilo", "hoilo",
            "holo", "nai", "nei", "chilo", "silo", "chhilo", "shobai", "sobai", "bhul", "deri", "dheri",
            "taratari", "mishti", "misti", "bosha", "boshar", "jaiga", "jayga", "poriccher", "poriskar",
            "nongra", "dhonnobad", "dhanyabad", "bhalobasha", "khete", "khelam", "kheyechi", "khaoya",
            "dam", "daam", "gorom", "thanda");

    public static String detect(String text) {
        if (text == null || text.isBlank()) return "en";

        boolean hasBanglaScript = text.codePoints().anyMatch(cp -> cp >= 0x0980 && cp <= 0x09FF);
        if (hasBanglaScript) return "bn";

        int total = 0;
        int hits = 0;
        for (String word : text.toLowerCase().split("[^a-z]+")) {
            if (word.isEmpty()) continue;
            total++;
            if (BANGLISH_WORDS.contains(word)) hits++;
        }
        if (hits >= 2 || (hits >= 1 && hits * 3 >= total)) return "banglish";
        return "en";
    }
}
