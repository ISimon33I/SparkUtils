package com.isimon33i.utils.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;

import com.isimon33i.utils.ChatUtils;

public class LanguageManager {

    public static final LanguageManager Instance = new LanguageManager();

    private final HashMap<String, Properties> languages = new HashMap<>();
    private File langFolder;

    public boolean debug;

    public void initialize(File langFolder) {
        this.langFolder = langFolder;
    }

    public boolean isInitialized() {
        return langFolder != null;
    }

    public String getMessage(String key, String locale, Placeholder... placeholders) {
        var messages = loadLanguage(locale);
        String message = messages.getProperty(key);
        if (message == null) {
            message = loadLanguage("en_us").getProperty(key);
            if (message == null) {
                message = key;
                if (debug) {
                    message += " | " + Arrays.stream(placeholders).map(x -> x.key + "=" + x.value).collect(Collectors.joining(", "));
                }
            }
        }

        for (var placeholder : placeholders) {
            message = message.replace("${" + placeholder.key + "}", placeholder.value);
        }

        return ChatUtils.hexColor(message);
    }

    private Properties loadLanguage(String locale) {
        if (languages.containsKey(locale)) {
            return languages.get(locale);
        }

        Properties messages = new Properties();

        var langFile = new File(langFolder, locale.toLowerCase() + ".txt");
        System.out.println("langfile path:" + langFile.getPath());
        if (langFile.exists()) {
            System.out.println("exists:" + langFile.getPath());
            try (var input = new FileInputStream(langFile)) {
                messages.load(new InputStreamReader(input, Charset.forName("UTF-8")));
            } catch (IOException e) {
                System.out.println("Could not load language file: " + e.getMessage());
            }

        }

        languages.put(locale, messages);
        return messages;
    }
}
