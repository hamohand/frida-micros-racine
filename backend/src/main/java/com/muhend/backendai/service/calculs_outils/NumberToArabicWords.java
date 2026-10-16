package com.muhend.backendai.service.calculs_outils;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NumberToArabicWords {

    private static final String[] CHIFFRES_ARABES = {
            "صفر",   // 0
            "واحد",  // 1
            "اثنان", // 2
            "ثلاثة", // 3
            "أربعة", // 4
            "خمسة",  // 5
            "ستة",   // 6
            "سبعة",  // 7
            "ثمانية",// 8
            "تسعة"   // 9
    };

    private static final String[] DIZAINES_ARABES = {
            "",       // 0
            "عشرة",   // 10
            "عشرون",  // 20
            "ثلاثون", // 30
            "أربعون", // 40
            "خمسون",  // 50
            "ستون",   // 60
            "سبعون",  // 70
            "ثمانون", // 80
            "تسعون"   // 90
    };

    private static final String[] DIZAINES_UNITE_ARABES = {
            "",           // 0
            "أحد عشر",    // 11
            "اثنا عشر",   // 12
            "ثلاثة عشر",  // 13
            "أربعة عشر",  // 14
            "خمسة عشر",   // 15
            "ستة عشر",    // 16
            "سبعة عشر",   // 17
            "ثمانية عشر", // 18
            "تسعة عشر"    // 19
    };

    private static final String[] CENTAINES_PREFIX = {
            "", "", "", "ثلاث", "أربع", "خمس", "ست", "سبع", "ثمان", "تسع"
    };

    public static String convert(int n) {
        if (n < 0) {
            return "ناقص " + convert(-n);
        }
        if (n == 0) {
            return "صفر";
        }
        if (n > 999_999_999) {
            return String.valueOf(n);
        }

        int millions = n / 1_000_000;
        int resteMillions = n % 1_000_000;

        int milliers = resteMillions / 1000;
        int resteMilliers = resteMillions % 1000;

        List<String> parties = new ArrayList<>();

        if (millions > 0) {
            if (millions == 1) {
                parties.add("مليون");
            } else if (millions == 2) {
                parties.add("مليونين");
            } else if (millions >= 3 && millions <= 10) {
                parties.add(traduireMoinsDeMille(millions) + " ملايين");
            } else {
                parties.add(traduireMoinsDeMille(millions) + " مليون");
            }
        }

        if (milliers > 0) {
            if (milliers == 1) {
                parties.add("ألف");
            } else if (milliers == 2) {
                parties.add("ألفين");
            } else if (milliers >= 3 && milliers <= 10) {
                parties.add(traduireMoinsDeMille(milliers) + " آلاف");
            } else {
                parties.add(traduireMoinsDeMille(milliers) + " ألف");
            }
        }

        if (resteMilliers > 0) {
            parties.add(traduireMoinsDeMille(resteMilliers));
        }

        return String.join(" و ", parties);
    }

    private static String traduireMoinsDeMille(int n) {
        if (n == 0) return "";
        List<String> resultat = new ArrayList<>();
        int centaines = n / 100;
        int reste = n % 100;

        if (centaines == 1) {
            resultat.add("مائة");
        } else if (centaines == 2) {
            resultat.add("مائتين");
        } else if (centaines >= 3 && centaines <= 9) {
            resultat.add(CENTAINES_PREFIX[centaines] + "مائة");
        }

        if (reste > 0) {
            if (reste < 10) {
                resultat.add(CHIFFRES_ARABES[reste]);
            } else if (reste >= 11 && reste <= 19) {
                resultat.add(DIZAINES_UNITE_ARABES[reste - 10]);
            } else {
                int dizaines = reste / 10;
                int unites = reste % 10;
                if (unites > 0) {
                    resultat.add(CHIFFRES_ARABES[unites] + " و " + DIZAINES_ARABES[dizaines]);
                } else {
                    resultat.add(DIZAINES_ARABES[dizaines]);
                }
            }
        }

        return String.join(" و ", resultat);
    }
}

