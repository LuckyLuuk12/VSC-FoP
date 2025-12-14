import java.util.*; 
/**
 * preSend(): Look for /set-colour or /set-color or /setcolor command and first argument as hex-color
 *            Then store that as ansi escape sequence in protected field currentColor and prepend to all outgoing messages
 *            in future calls to preSend()
 */
public   class  Client {
	
  /// The ANSI escape sequence representing a hex color, e.g. "\u001B[38;2;255;0;0m" for red
  protected String currentColor  ;

	
  /// A list of command aliases to set color
  public static final List<String> COLOR_COMMANDS  = Arrays.asList("/set-color", "/set-colour", "/setcolor", "/setcolour", "/colour", "/color");

	

	

	

  private String  preSend__wrappee__src  (String message) {
    if (message == null) return null;

    // If UI prefixed a name using the client separator, only examine the payload part
    String payload = message;
    String[] parts = message.split(java.util.regex.Pattern.quote(SEPARATOR), 2);
    if (parts.length == 2) payload = parts[1];

    String trimmed = payload.trim();
    // split into command and rest (at most 2 tokens)
    String[] tokens = trimmed.split("\\s+", 2);
    String cmd = tokens.length > 0 ? tokens[0].toLowerCase() : "";

    if (COLOR_COMMANDS.contains(cmd)) {
      // ensure there is at least one argument (the color)
      String arg = tokens.length > 1 ? tokens[1].trim() : "";
      String hex = normalizeColorArg(arg);
      if (hex != null) {
        currentColor = asANSI(hex);
      } else {
        currentColor = null; // reset color
      }
      System.out.println("Color set to " + (hex != null ? hex : "none"));
      return null; // consume the command
    }

    // Let other refinements run first
    String processed = original(message);
    if (processed == null) return null;

    if (currentColor != null) {
      processed = currentColor + processed + "\u001B[0m"; // append reset code
    }
    return processed;
  }

	

	

  private String  preSend__wrappee__src  (String message) {
    if (message == null) return null;

    // If UI prefixed a name using the client separator, only examine the payload part
    String payload = message;
    String[] parts = message.split(java.util.regex.Pattern.quote(SEPARATOR), 2);
    if (parts.length == 2) payload = parts[1];

    String trimmed = payload.trim();
    // split into command and rest (at most 2 tokens)
    String[] tokens = trimmed.split("\\s+", 2);
    String cmd = tokens.length > 0 ? tokens[0].toLowerCase() : "";

    if (COLOR_COMMANDS.contains(cmd)) {
      // ensure there is at least one argument (the color)
      String arg = tokens.length > 1 ? tokens[1].trim() : "";
      String hex = normalizeColorArg(arg);
      if (hex != null) {
        currentColor = asANSI(hex);
      } else {
        currentColor = null; // reset color
      }
      System.out.println("Color set to " + (hex != null ? hex : "none"));
      return null; // consume the command
    }

    // Let other refinements run first
    String processed = preSend__wrappee__src(message);
    if (processed == null) return null;

    if (currentColor != null) {
      processed = currentColor + processed + "\u001B[0m"; // append reset code
    }
    return processed;
  }

	

  protected String preSend  (String message) {
    if (message == null) return null;

    // If UI prefixed a name using the client separator, only examine the payload part
    String payload = message;
    String[] parts = message.split(java.util.regex.Pattern.quote(SEPARATOR), 2);
    if (parts.length == 2) payload = parts[1];

    String trimmed = payload.trim();
    // split into command and rest (at most 2 tokens)
    String[] tokens = trimmed.split("\\s+", 2);
    String cmd = tokens.length > 0 ? tokens[0].toLowerCase() : "";

    if (COLOR_COMMANDS.contains(cmd)) {
      // ensure there is at least one argument (the color)
      String arg = tokens.length > 1 ? tokens[1].trim() : "";
      String hex = normalizeColorArg(arg);
      if (hex != null) {
        currentColor = asANSI(hex);
      } else {
        currentColor = null; // reset color
      }
      System.out.println("Color set to " + (hex != null ? hex : "none"));
      return null; // consume the command
    }

    // Let other refinements run first
    String processed = preSend__wrappee__src(message);
    if (processed == null) return null;

    if (currentColor != null) {
      processed = currentColor + processed + "\u001B[0m"; // append reset code
    }
    return processed;
  }

	

  private String normalizeColorArg  (String arg) {
    if (arg == null) return null;
    arg = arg.trim().toLowerCase();
    if (arg.isEmpty()) return null;
    if (arg.startsWith("#")) {
      if (arg.length() == 4) {
        char r = arg.charAt(1), g = arg.charAt(2), b = arg.charAt(3);
        return String.format("#%c%c%c%c%c%c", r, r, g, g, b, b).toUpperCase();
      } else if (arg.length() == 7) {
        return arg.toUpperCase();
      }
      return null;
    }
    // Add more named colors if desired
    switch (arg) {
      case "red": return "#FF0000";
      case "green": return "#00FF00";
      case "blue": return "#0000FF";
      case "yellow": return "#FFFF00";
      case "cyan": return "#00FFFF";
      case "magenta": return "#FF00FF";
      case "black": return "#000000";
      case "white": return "#FFFFFF";
      case "gray":
      case "grey": return "#808080";
      default: return null;
    }
  }

	

  private String asANSI  (String hexColor) {
    if (hexColor == null || !hexColor.startsWith("#") || hexColor.length() != 7) return null;
    try {
      int r = Integer.parseInt(hexColor.substring(1, 3), 16);
      int g = Integer.parseInt(hexColor.substring(3, 5), 16);
      int b = Integer.parseInt(hexColor.substring(5, 7), 16);
      return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    } catch (NumberFormatException e) {
      return null;
    }
  }


}
