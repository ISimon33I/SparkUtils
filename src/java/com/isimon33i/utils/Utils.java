package com.isimon33i.utils;

import java.util.List;

public class Utils {
    private Utils(){}
    
    public static List<String> filterByStart(String startsWith, List<String> input, boolean caseSensitive) {
        if(caseSensitive){
            return input.stream().filter(x -> x.startsWith(startsWith)).toList();
        }else{
            var startsWithLowerCase = startsWith.toLowerCase();
            return input.stream().filter(x -> x.toLowerCase().startsWith(startsWithLowerCase)).toList();
        }
    }
}