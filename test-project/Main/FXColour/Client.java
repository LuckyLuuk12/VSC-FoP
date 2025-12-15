import java.util.Arrays; 

/**
 * FXColour feature:
 * - refine preSend to handle /setcolor commands and store protected currentColor (hex #RRGGBB)
 * - refine postSend to append " ||color=#RRGGBB" to outgoing messages when currentColor set,
 *   and to call GUI.appendMessage(text, css) to render locally with color
 * - refine receive to strip " ||color=" suffix, render via GUI.appendMessage(text, css) and consume
 *
 * Separation: GUI contains only display API; FXColour handles color semantics and uses GUI append API.
 */
public   class  Client {
	
  // protected field available to composition; FXColour sets it
  protected String currentColor  = null;

	

  // avoid generic diamond usage for older compiler levels
  public static final java.util.List COLOR_COMMANDS  = Arrays.asList(
    "/set-color", "/set-colour", "/setcolor", "/setcolour", "/colour", "/color"
  );

	

	

	

  private String  preSend__wrappee__src  (String message) {
    if (message == null) return null;

    // If UI prefixed a name using SEPARATOR, only examine payload
    String payload = message;
    try {
      String[] parts = message.split(java.util.regex.Pattern.quote(SEPARATOR), 2);
      if (parts.length == 2) payload = parts[1];
    } catch (Throwable ignored) {}

    String trimmed = payload.trim();
    String[] tokens = trimmed.split("\\s+", 2);
    String cmd = tokens.length > 0 ? tokens[0].toLowerCase() : "";

    if (COLOR_COMMANDS.contains(cmd)) {
      String arg = tokens.length > 1 ? tokens[1].trim() : "";
      String hex = normalizeColorArg(arg);
      if (hex != null) {
        this.currentColor = hex;
        System.out.println("FXColour: color set to " + hex);
      } else {
        this.currentColor = null;
        System.out.println("FXColour: color cleared");
      }
      // consume the command
      return null;
    }

    // for non-command messages, let other refinements run first
    return original(message);
  }

	

	

  private String  preSend__wrappee__src  (String message) {
    if (message == null) return null;

    // If UI prefixed a name using SEPARATOR, only examine payload
    String payload = message;
    try {
      String[] parts = message.split(java.util.regex.Pattern.quote(SEPARATOR), 2);
      if (parts.length == 2) payload = parts[1];
    } catch (Throwable ignored) {}

    String trimmed = payload.trim();
    String[] tokens = trimmed.split("\\s+", 2);
    String cmd = tokens.length > 0 ? tokens[0].toLowerCase() : "";

    if (COLOR_COMMANDS.contains(cmd)) {
      String arg = tokens.length > 1 ? tokens[1].trim() : "";
      String hex = normalizeColorArg(arg);
      if (hex != null) {
        this.currentColor = hex;
        System.out.println("FXColour: color set to " + hex);
      } else {
        this.currentColor = null;
        System.out.println("FXColour: color cleared");
      }
      // consume the command
      return null;
    }

    // for non-command messages, let other refinements run first
    return preSend__wrappee__src(message);
  }

	

  protected String preSend  (String message) {
    if (message == null) return null;

    // If UI prefixed a name using SEPARATOR, only examine payload
    String payload = message;
    try {
      String[] parts = message.split(java.util.regex.Pattern.quote(SEPARATOR), 2);
      if (parts.length == 2) payload = parts[1];
    } catch (Throwable ignored) {}

    String trimmed = payload.trim();
    String[] tokens = trimmed.split("\\s+", 2);
    String cmd = tokens.length > 0 ? tokens[0].toLowerCase() : "";

    if (COLOR_COMMANDS.contains(cmd)) {
      String arg = tokens.length > 1 ? tokens[1].trim() : "";
      String hex = normalizeColorArg(arg);
      if (hex != null) {
        this.currentColor = hex;
        System.out.println("FXColour: color set to " + hex);
      } else {
        this.currentColor = null;
        System.out.println("FXColour: color cleared");
      }
      // consume the command
      return null;
    }

    // for non-command messages, let other refinements run first
    return preSend__wrappee__src(message);
  }

	

	

	

  private String  postSend__wrappee__src  (String message) {
    if (message == null) return null;

    // let lower refinements run first
    String processed = original(message);
    if (processed == null) return null;

    // If we have a currentColor, append meta to wire form
    if (this.currentColor != null && this.currentColor.length() > 0) {
      String wire = processed;
      if (wire.indexOf(" ||color=") < 0) {
        wire = wire + " ||color=" + this.currentColor;
      }

      // Local colored display: try to call GUI append API (GUI provides appendMessage)
      try {
        String css = "-fx-text-fill: " + this.currentColor + ";";
        SwingApp.appendMessage(processed, css);
      } catch (Throwable ignored) {
        // GUI may not be present; ignore
      }

      return wire;
    }

    // no color: return processed as-is
    return processed;
  }

	

	

  private String  postSend__wrappee__src  (String message) {
    if (message == null) return null;

    // let lower refinements run first
    String processed = postSend__wrappee__src(message);
    if (processed == null) return null;

    // If we have a currentColor, append meta to wire form
    if (this.currentColor != null && this.currentColor.length() > 0) {
      String wire = processed;
      if (wire.indexOf(" ||color=") < 0) {
        wire = wire + " ||color=" + this.currentColor;
      }

      // Local colored display: try to call GUI append API (GUI provides appendMessage)
      try {
        String css = "-fx-text-fill: " + this.currentColor + ";";
        SwingApp.appendMessage(processed, css);
      } catch (Throwable ignored) {
        // GUI may not be present; ignore
      }

      return wire;
    }

    // no color: return processed as-is
    return processed;
  }

	

  protected String postSend  (String message) {
    if (message == null) return null;

    // let lower refinements run first
    String processed = postSend__wrappee__src(message);
    if (processed == null) return null;

    // If we have a currentColor, append meta to wire form
    if (this.currentColor != null && this.currentColor.length() > 0) {
      String wire = processed;
      if (wire.indexOf(" ||color=") < 0) {
        wire = wire + " ||color=" + this.currentColor;
      }

      // Local colored display: try to call GUI append API (GUI provides appendMessage)
      try {
        String css = "-fx-text-fill: " + this.currentColor + ";";
        SwingApp.appendMessage(processed, css);
      } catch (Throwable ignored) {
        // GUI may not be present; ignore
      }

      return wire;
    }

    // no color: return processed as-is
    return processed;
  }

	

	

	

  private String  receive__wrappee__src  (String message) {
    if (message == null) return null;

    // allow other refinements (decryption, etc.) to run first
    String processed = original(message);
    if (processed == null) return null;

    int idx = processed.lastIndexOf(" ||color=");
    if (idx < 0) {
      return processed;
    }

    String text = processed.substring(0, idx);
    String hex = processed.substring(idx + " ||color=".length()).trim();

    // normalise short form #RGB to #RRGGBB
    if (hex != null && hex.length() == 4 && hex.charAt(0) == '#') {
      char r = hex.charAt(1), g = hex.charAt(2), b = hex.charAt(3);
      hex = String.format("#%c%c%c%c%c%c", r, r, g, g, b, b).toUpperCase();
    } else {
      try { hex = hex.toUpperCase(); } catch (Throwable ignored) {}
    }

    final String css = "-fx-text-fill: " + hex + ";";

    // Use GUI append API to display colored message and consume (so GUI doesn't display plain copy)
    try {
      SwingApp.appendMessage(text, css);
      return null; // consumed
    } catch (Throwable ignored) {
      // if GUI not present, fall back to returning cleaned text
    }

    return text;
  }

	

	

  private String  receive__wrappee__src  (String message) {
    if (message == null) return null;

    // allow other refinements (decryption, etc.) to run first
    String processed = receive__wrappee__src(message);
    if (processed == null) return null;

    int idx = processed.lastIndexOf(" ||color=");
    if (idx < 0) {
      return processed;
    }

    String text = processed.substring(0, idx);
    String hex = processed.substring(idx + " ||color=".length()).trim();

    // normalise short form #RGB to #RRGGBB
    if (hex != null && hex.length() == 4 && hex.charAt(0) == '#') {
      char r = hex.charAt(1), g = hex.charAt(2), b = hex.charAt(3);
      hex = String.format("#%c%c%c%c%c%c", r, r, g, g, b, b).toUpperCase();
    } else {
      try { hex = hex.toUpperCase(); } catch (Throwable ignored) {}
    }

    final String css = "-fx-text-fill: " + hex + ";";

    // Use GUI append API to display colored message and consume (so GUI doesn't display plain copy)
    try {
      SwingApp.appendMessage(text, css);
      return null; // consumed
    } catch (Throwable ignored) {
      // if GUI not present, fall back to returning cleaned text
    }

    return text;
  }

	

  protected String receive  (String message) {
    if (message == null) return null;

    // allow other refinements (decryption, etc.) to run first
    String processed = receive__wrappee__src(message);
    if (processed == null) return null;

    int idx = processed.lastIndexOf(" ||color=");
    if (idx < 0) {
      return processed;
    }

    String text = processed.substring(0, idx);
    String hex = processed.substring(idx + " ||color=".length()).trim();

    // normalise short form #RGB to #RRGGBB
    if (hex != null && hex.length() == 4 && hex.charAt(0) == '#') {
      char r = hex.charAt(1), g = hex.charAt(2), b = hex.charAt(3);
      hex = String.format("#%c%c%c%c%c%c", r, r, g, g, b, b).toUpperCase();
    } else {
      try { hex = hex.toUpperCase(); } catch (Throwable ignored) {}
    }

    final String css = "-fx-text-fill: " + hex + ";";

    // Use GUI append API to display colored message and consume (so GUI doesn't display plain copy)
    try {
      SwingApp.appendMessage(text, css);
      return null; // consumed
    } catch (Throwable ignored) {
      // if GUI not present, fall back to returning cleaned text
    }

    return text;
  }

	

  private String normalizeColorArg  (String arg) {
    if (arg == null) return null;
    arg = arg.trim().toLowerCase();
    if (arg.length() == 0) return null;
    if (arg.startsWith("#")) {
      if (arg.length() == 4) {
        char r = arg.charAt(1), g = arg.charAt(2), b = arg.charAt(3);
        return String.format("#%c%c%c%c%c%c", r, r, g, g, b, b).toUpperCase();
      } else if (arg.length() == 7) {
        return arg.toUpperCase();
      }
      return null;
    }
    if ("red".equals(arg)) return "#FF0000";
    if ("green".equals(arg)) return "#00FF00";
    if ("blue".equals(arg)) return "#0000FF";
    if ("yellow".equals(arg)) return "#FFFF00";
    if ("cyan".equals(arg)) return "#00FFFF";
    if ("magenta".equals(arg)) return "#FF00FF";
    if ("white".equals(arg)) return "#FFFFFF";
    if ("black".equals(arg)) return "#000000";
    return null;
  }


}
