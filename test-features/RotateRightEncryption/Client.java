public class Client {
  protected static final int ROTATION = 3;


  /**
   * ?NOTE: We encrypt messages after UI-handling before sending to the server.
   * @param message the message to encrypt.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  protected String postSend(String message) {
    if (message == null) return null;
    // let other refinements run first
    String processed = original(message);
    if (processed == null) return null;

    StringBuilder encrypted = new StringBuilder();
    for (char c : processed.toCharArray()) {
      if (Character.isLetter(c)) {
        char base = Character.isLowerCase(c) ? 'a' : 'A';
        c = (char) ((c - base + ROTATION) % 26 + base);
      }
      encrypted.append(c);
    }
    return encrypted.toString();
  }


  /**
   * ?NOTE: We decrypt messages before UI-handling when receiving from the server.
   * @param message the message to decrypt.
   * @return the processed message (can be the same as input). If null is returned the message is not sent/displayed.
   */  
  protected String preReceive(String message) {
    if (message == null) return null;
    // decrypt first so original(...) operates on plaintext
    StringBuilder decrypted = new StringBuilder();
    for (char c : message.toCharArray()) {
      if (Character.isLetter(c)) {
        char base = Character.isLowerCase(c) ? 'a' : 'A';
        c = (char) ((c - base - ROTATION + 26) % 26 + base);
      }
      decrypted.append(c);
    }
    return original(decrypted.toString());
  }

}