package com.isimon33i.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatUtils {
    private ChatUtils(){}
    
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("[&§]#[a-fA-F0-9]{6}");

    public static String hexColor(String message) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(message);

        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, "" + ChatColor.of(color.substring(1)));
            matcher = HEX_COLOR_PATTERN.matcher(message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Sends a clickable message to a player that runs a command when clicked.
     * @param message The clickable message!
     * @param command The command without the slash to make the user perform.
     * @param player player to send to.
     */
    public static void sendClickableCommand(Player player, String message, String command) {
        // Make a new component (Bungee API).
        TextComponent component = new TextComponent(new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
        // Add a click event to the component.
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command));

        // Send it!
        player.spigot().sendMessage(component);
    }

    public static void sendTwoButtonMessage(Player player, String message, String btn1Txt, String btn2Txt, String btn1Command, String btn2Command) {
        // Make a new component (Bungee API).
        TextComponent messageComponent = new TextComponent(new TextComponent(ChatUtils.hexColor(message)));

        TextComponent acceptComponent = new TextComponent(new TextComponent(ChatUtils.hexColor(btn1Txt)));
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + btn1Command));

        TextComponent denyComponent = new TextComponent(new TextComponent(ChatUtils.hexColor(btn2Txt)));
        denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + btn2Command));
        
        
        TextComponent component = new TextComponent(messageComponent, new TextComponent("\n "), acceptComponent, new TextComponent("   "), denyComponent);

        // Send it!
        player.spigot().sendMessage(component);
    }
    
    public static List<String> parseQuotedArgs(String[] args) {
        String input = String.join(" ", args);
        List<String> result = new ArrayList<>();

        // Regex för att matcha: "citerad text" eller vanliga ord, hanterar \"
        Pattern pattern = Pattern.compile("\"((?:\\\\\"|[^\"])+)\"|(\\S+)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Byt ut \" med riktig "
                result.add(matcher.group(1).replace("\\\"", "\""));
            } else {
                result.add(matcher.group(2));
            }
        }

        return result;
    }
}
