package com.muhend.backendai.service.calculs_outils;

import org.springframework.stereotype.Service;

@Service
public class NumberToArabicWords {

    private static final String[] units = {
            "", "واحد", "اثنين", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"
    };
    private static final String[] tens = {
            "", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون",
            "ستون", "سبعون", "ثمانون", "تسعون"
    };
    private static final String[] teens = {
            "عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر",
            "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    };
    private static final String[] thousands = {
            "", "ألف", "مليون", "مليار", "تريليون"
    };

    public static String convert(int number) {
        if (number == 0) {
            return "صفر";
        }
        if (number < 0) {
            return "ناقص " + convert(-number);
        }

        String words = "";

        int place = 0;
        do {
            int n = number % 1000;
            if (n != 0) {
                String segment = convertLessThanOneThousand(n);
                words = segment + " " + thousands[place] + " " + words;
            }
            place++;
            number /= 1000;
        } while (number > 0);

        return words.trim();
    }

    private static String convertLessThanOneThousand(int number) {
        String current;

        if (number % 100 < 10) {
            current = units[number % 10];
            number /= 10;
        } else if (number % 100 < 20) {
            current = teens[number % 10];
            number /= 100;
        } else {
            current = units[number % 10];
            number /= 10;

            //current = tens[number % 10] + " " + current;
            current =  current + " و " + tens[number % 10];
            number /= 10;
        }

        if (number == 0) {
            return current;
        }
        return units[number] + " مئة و " + current;

    }

    /*public static void main(String[] args) {
        System.out.println(convert(3652885)); // Output: ثلاثة مليون ستة مئة و اثنين و خمسون ألف ثمانية مئة و خمسة و ثمانون
    }*/


}
