import java.io.*; 
import java.net.*; 
import java.nio.charset.StandardCharsets; import java.util.*; 
import java.util.concurrent.*; 

public   class  Server {
	
  public static final String SEPARATOR = " » ";

	
  /// A hard-coded host which the server and its sub-features (like the client) will use.
  protected final String HOST = "localhost";

	
  /// A hard-coded port which the server and its sub-features (like the client) will use.
  protected final int PORT = 1234;

	

  /// Thread pool to handle multiple clients' threads/connection sockets
  protected final ExecutorService clientThreads = Executors.newCachedThreadPool();

	
  /// Thread pool to handle multiple clients
  protected final Set<Runnable> clientHandlers = ConcurrentHashMap.newKeySet();

	
  /// A list of currently connected sockets (for broadcasting)
  protected final Set<Socket> clientSockets = ConcurrentHashMap.newKeySet();

	


  public void startServer() throws Exception {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(PORT);
      System.out.println("Server listening on " + PORT);

      while(!Thread.currentThread().isInterrupted()) {
        final Socket socket = serverSocket.accept();
        clientSockets.add(socket); // <-- Track new connection

        Runnable client = new Runnable() {
          @Override
          public void run() {
            BufferedReader in = null;
            try {
              in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
              String message;
              while ((message = in.readLine()) != null) {
                // Here the server receives a String, we can handleRequest it to process before sending to other clients
                String response = handleRequest(message);
                sendToAllClients(response, socket); // broadcast to all other clients except the sender
              }
            } catch (IOException e) {
              e.printStackTrace();
            } finally {
              // Clean up resources and remove socket from active connections
              if (in != null) {
                try {
                  in.close();
                } catch (IOException e) {
                  // Ignore
                }
              }
              try {
                socket.close();
              } catch (IOException e) {
                e.printStackTrace();
              }
              clientSockets.remove(socket); // <-- Remove on disconnect
            }
          }
        };

        clientHandlers.add(client);
        clientThreads.submit(client);
      }

    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      this.stopServer();
    }
  }

	

  public void stopServer() throws Exception {
    clientThreads.shutdownNow();
  }

	

  /**
   * This function handles incoming messages from clients. It can be overridden to process messages before handling them.
   * @param message the message received from a client.
   */
  private String  handleRequest__wrappee__CLI  (String message) {
    System.out.println(message);
    // After processing the message we can send it to all connected clients.
    return message;
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
      return handleRequest__wrappee__CLI(message);
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

	

  /**
   * This function sends a message to all connected clients.
   * @param message the message to send to all clients.
   */
  protected void sendToAllClients(String message, Socket... exclude) {
    if (message == null) return;
    for (Iterator<Socket> it = clientSockets.iterator(); it.hasNext();) {
      Socket socket = it.next();
      if (Arrays.asList(exclude).contains(socket)) continue; // Skip excluded sockets
      try {
        OutputStream out = socket.getOutputStream();
        out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
      } catch (IOException e) {
        it.remove(); // remove dead connections
      }
    }
  }

	
  protected static Map<String, String> PASSWORDS = new HashMap<String,String>();

	
  protected static Set<String> AUTHENTICATED = Collections.synchronizedSet(new HashSet<String>());

	

  static {
    Map<String, String> p = new HashMap<String,String>();
    p.put("alice", "alicepass");
    p.put("bob", "bobpass");
    p.put("charlie", "charliepass");
    PASSWORDS = Collections.unmodifiableMap(p);
  }


}
