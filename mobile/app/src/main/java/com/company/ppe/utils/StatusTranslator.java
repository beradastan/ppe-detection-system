package com.company.ppe.utils;

import java.util.HashMap;
import java.util.Map;

public class StatusTranslator {
    private static final Map<String, String> translations = new HashMap<>();
    
    static {
        // Compliance status translations from English to Turkish
        translations.put("full_compliance", "Tam uyum");
        translations.put("partial_compliance", "Kısmi uyum");
        translations.put("non_compliance", "Uyumsuzluk");
        translations.put("no_person", "Kişi algılanmadı");
        translations.put("error", "Tespit hatası");
    }
    
    /**
     * Translates status from English to Turkish
     * @param englishStatus Status in English
     * @return Turkish translation or original status if no translation found
     */
    public static String translate(String englishStatus) {
        if (englishStatus == null || englishStatus.trim().isEmpty()) {
            return englishStatus;
        }
        
        String normalized = englishStatus.toLowerCase().trim();
        String translation = translations.get(normalized);
        
        return translation != null ? translation : englishStatus;
    }
}
