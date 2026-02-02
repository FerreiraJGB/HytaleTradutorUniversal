package com.jogandobem.discord;

public final class DiscordMessageSanitizer {
   private static final int DISCORD_MAX_LENGTH = 2000;
   private static final int GAME_MAX_LENGTH = 256;
   private static final String ZWSP = "\u200B";

   private DiscordMessageSanitizer() {
   }

   public static String sanitizeForDiscord(String input) {
      if (input == null || input.isEmpty()) {
         return "";
      }
      String result = input;
      result = result.replace("<@&", "<@" + ZWSP + "&");
      result = result.replaceAll("[?][0-9a-fk-or]", "");
      result = result.replaceAll("(?<!" + ZWSP + ")&[0-9a-fk-or]", "");
      result = result.replace("@everyone", "@" + ZWSP + "everyone");
      result = result.replace("@here", "@" + ZWSP + "here");
      if (result.length() > DISCORD_MAX_LENGTH) {
         result = result.substring(0, DISCORD_MAX_LENGTH - 3) + "...";
      }
      return result;
   }

   public static String sanitizeForGame(String input) {
      if (input == null || input.isEmpty()) {
         return "";
      }
      String result = input;
      result = result.replaceAll("```[\\s\\S]*?```", "[code]");
      result = result.replaceAll("`[^`]+`", "[code]");
      result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
      result = result.replaceAll("__(.+?)__", "$1");
      result = result.replaceAll("~~(.+?)~~", "$1");
      result = result.replaceAll("\\*(.+?)\\*", "$1");
      result = result.replaceAll("_(.+?)_", "$1");
      result = result.replaceAll("\\|\\|(.+?)\\|\\|", "$1");
      if (result.length() > GAME_MAX_LENGTH) {
         result = result.substring(0, GAME_MAX_LENGTH - 3) + "...";
      }
      return result;
   }
}

