import java.util.*;

public class Server {
  protected static Map<String, String> PASSWORDS = new HashMap<String,String>();
  protected static Set<String> AUTHENTICATED = Collections.synchronizedSet(new HashSet<String>());

  static {
    Map<String, String> p = new HashMap<String,String>();
    p.put("alice", "alicepass");
    p.put("bob", "bobpass");
    p.put("charlie", "charliepass");
    PASSWORDS = Collections.unmodifiableMap(p);
  }

  /**
   * We use SEPARATOR to separate username and message, if the user is not authenticated yet
   * then we expect the message to be "username<SEPARATOR>password" and authenticate the user.
   * If the user is authenticated we just pass the message through using original(...)
   * @param message the message to authenticate.
   * @return the processed message (can be the same as input). If null is returned
   */
  protected String handleRequest(String message) {
    if (message == null) return null;
    String[] parts = message.split(SEPARATOR, 2);
    if (parts.length != 2) {
      System.err.println("Invalid message format. Use: username" + SEPARATOR + "password");
      return null;
    }
    String username = parts[0];
    String password = parts[1];
    if (AUTHENTICATED.contains(username)) {
      // already authenticated, just pass through
      return original(message);
    }
    String expectedPassword = PASSWORDS.get(username);
    if (expectedPassword == null) {
      System.err.println("Unknown user '" + username + "'.");
      return null; // do not send the failed authentication message to the server
    }

    if (expectedPassword.equals(password)) {
      AUTHENTICATED.add(username);
      System.out.println("User '" + username + "' authenticated successfully.");

      sendToAllClients(username + " is authenticated!");
      return null; // do not send the authentication message to the server
    } else {
      System.err.println("Authentication failed for user '" + username + "'.");
      sendToAllClients(username + " failed to authenticate!");
      return null; // do not send the failed authentication message to the server
    }
  }
/*
  protected void onStop(Socket socket) {
    // call refinements below/previous in the chain first
    original(socket);
    // then if name != null remove from authenticated set
    if (name != null) {
      AUTHENTICATED.remove(name);
      System.out.println("User '" + name + "' logged out.");
    }
  }
*/
}