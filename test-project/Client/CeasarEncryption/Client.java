public   class  Client {
	

  protected static final int ROTATION  = 13;

	

  public static char rotateChar  (char c, int rotation) {
    if (c >= 'a' && c <= 'z') {
      return (char) ('a' + (c - 'a' + rotation + 26) % 26);
    } else if (c >= 'A' && c <= 'Z') {
      return (char) ('A' + (c - 'A' + rotation + 26) % 26);
    } else {
      return c;
    }
  }

	

  public static String rotateString  (String input, int rotation) {
    StringBuilder result = new StringBuilder();
    for (char c : input.toCharArray()) {
      result.append(rotateChar(c, rotation));
    }
    return result.toString();
  }

	

	

  private String  postSend__wrappee__src  (String message) {
    if (message == null) return null;
    // let original refinements run first, then encrypt the resulting text
    String processed = original(message);
    if (processed == null) return null;
    return rotateString(processed, ROTATION);
  }

	

  protected String postSend  (String message) {
    if (message == null) return null;
    // let original refinements run first, then encrypt the resulting text
    String processed = postSend__wrappee__src(message);
    if (processed == null) return null;
    return rotateString(processed, ROTATION);
  }

	

	

  private String  preReceive__wrappee__src  (String message) {
    if (message == null) return null;
    // decrypt first so original refinements operate on plaintext
    String decrypted = rotateString(message, -ROTATION);
    return original(decrypted);
  }

	

  protected String preReceive  (String message) {
    if (message == null) return null;
    // decrypt first so original refinements operate on plaintext
    String decrypted = rotateString(message, -ROTATION);
    return preReceive__wrappee__src(decrypted);
  }


}
