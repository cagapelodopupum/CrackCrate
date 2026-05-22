package com.crackcrate.utils;

import org.bukkit.ChatColor;
import java.util.List;
import java.util.stream.Collectors;

public class ColorUtils {
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorizeList(List<String> list) {
        return list.stream().map(ColorUtils::colorize).collect(Collectors.toList());
    }
}
