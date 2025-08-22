package com.company.ppe.utils;

import java.util.HashMap;
import java.util.Map;

public class EquipmentTranslator {
    private static final Map<String, String> translations = new HashMap<>();
    
    static {
        // PPE Equipment translations from English to Turkish
        translations.put("helmet", "Kask");
        translations.put("hard_hat", "Kask");
        translations.put("safety_helmet", "Güvenlik Kaskı");
        translations.put("gloves", "Eldiven");
        translations.put("safety_gloves", "Eldiven");
        translations.put("vest", "Yelek");
        translations.put("safety_vest", "Güvenlik Yelegi");
        translations.put("high_vis_vest", "Görünürlük Yelegi");
        translations.put("reflective_vest", "Reflektör Yelek");
        translations.put("boots", "Bot");
        translations.put("safety_boots", "Güvenlik Botu");
        translations.put("steel_toe_boots", "Çelik Burunlu Bot");
        translations.put("goggles", "Gözlük");
        translations.put("safety_goggles", "Gözlük");
        translations.put("glasses", "Gözlük");
        translations.put("safety_glasses", "Gözlük");
        translations.put("eye_wear", "Gözlük");
        translations.put("eyewear", "Gözlük");
        translations.put("eye wear", "Gözlük");
        translations.put("mask", "Maske");
        translations.put("face_mask", "Yüz Maskesi");
        translations.put("respirator", "Solunum Maskesi");
        translations.put("ear_protection", "Kulak Koruması");
        translations.put("earplugs", "Kulak Tıkacı");
        translations.put("earmuffs", "Kulak Koruyucu");
        translations.put("harness", "Emniyet Kemeri");
        translations.put("safety_harness", "Güvenlik Kemeri");
        translations.put("fall_protection", "Düşme Koruması");
        translations.put("shield", "Siperlik");
        translations.put("face_shield", "Siperlik");
        translations.put("safety_shield", "Siperlik");
        translations.put("person", "İnsan");
        
        // Common variations and synonyms
        translations.put("hard hat", "Kask");
        translations.put("safety helmet", "Güvenlik Kaskı");
        translations.put("safety gloves", "Eldiven");
        translations.put("safety vest", "Güvenlik Yelegi");
        translations.put("high vis vest", "Görünürlük Yelegi");
        translations.put("reflective vest", "Reflektör Yelek");
        translations.put("safety boots", "Güvenlik Botu");
        translations.put("steel toe boots", "Çelik Burunlu Bot");
        translations.put("safety goggles", "Gözlük");
        translations.put("safety glasses", "Gözlük");
        translations.put("face mask", "Yüz Maskesi");
        translations.put("ear protection", "Kulak Koruması");
        translations.put("safety harness", "Güvenlik Kemeri");
        translations.put("fall protection", "Düşme Koruması");
        
        // Additional common equipment
        translations.put("coveralls", "Tulum");
        translations.put("apron", "Önlük");
        translations.put("knee_pads", "Diz Koruyucu");
        translations.put("knee pads", "Diz Koruyucu");
        translations.put("elbow_pads", "Dirsek Koruyucu");
        translations.put("elbow pads", "Dirsek Koruyucu");
        translations.put("shin_guards", "Kaval Kemiği Koruyucu");
        translations.put("shin guards", "Kaval Kemiği Koruyucu");
    }
    
    /**
     * Translates equipment name from English to Turkish
     * @param englishName Equipment name in English
     * @return Turkish translation or original name if no translation found
     */
    public static String translate(String englishName) {
        if (englishName == null || englishName.trim().isEmpty()) {
            return englishName;
        }
        
        String normalized = englishName.toLowerCase().trim();
        
        // Direct translation lookup
        String translation = translations.get(normalized);
        if (translation != null) {
            return translation;
        }
        
        // Try with underscores replaced by spaces
        String withSpaces = normalized.replace("_", " ");
        translation = translations.get(withSpaces);
        if (translation != null) {
            return translation;
        }
        
        // Try with spaces replaced by underscores
        String withUnderscores = normalized.replace(" ", "_");
        translation = translations.get(withUnderscores);
        if (translation != null) {
            return translation;
        }
        
        // Partial matching for compound words
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }
        
        // Return original name with proper capitalization if no translation found
        return capitalizeWords(englishName);
    }
    
    /**
     * Capitalizes the first letter of each word
     */
    private static String capitalizeWords(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String[] words = input.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            String word = words[i];
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
}
