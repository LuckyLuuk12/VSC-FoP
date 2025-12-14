import java.io.*; 
import java.net.*; 
import java.util.concurrent.*; 

public   class  Client {
	
  protected void onStart(Socket socket) {
    // The CLI creates a thread to read messages from the server and print them to the console.
    // It calls receive() on the String message received from the server such that other features can override it.
    Thread readerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        //! Because FeatureIDE and FeatureHouse are SO EXTREMELY OUTDATED we cannot use lambdas and try-with-resources here...
        BufferedReader in = null;
        try {
          in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          String message;
          while ((message = in.readLine()) != null) {
            // Call receive to process the message and print it
            receive(message);
          }
        } catch (IOException e) {
          System.err.println("Error reading from server: " + e.getMessage());
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
              // Ignore
            }
          }
        }
      }
    });
    readerThread.start();
    // The CLI creates a thread to read user input from the console and send it to the server.
    // It calls send() to send the message such that other features can override it.
    Thread writerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        PrintWriter out = null;
        BufferedReader userInput = null;
        try {
          out = new PrintWriter(socket.getOutputStream(), true);
          userInput = new BufferedReader(new InputStreamReader(System.in));
          String input;
          while ((input = userInput.readLine()) != null) {
            String toSend = send(name + SEPARATOR + input); // process, print, and get final message
            if (toSend != null) { // only send if not nullified
              out.println(toSend); // actually send the message to the server
            }
          }
        } catch (IOException e) {
          System.err.println("Error writing to server: " + e.getMessage());
        } finally {
          if (out != null) {
            out.close();
          }
          if (userInput != null) {
            try {
              userInput.close();
            } catch (IOException e) {
              // Ignore
            }
          }
        }
      }
    });
    writerThread.start();
  }

	

	

	

  /**
   * ?NOTE: This CLI feature implements send() by printing to console after preSend() and before postSend().
   * This function sends a message to the server.
   * @param message the message to process and send to the server.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  private String  send__wrappee__src  (String message) {
    // Should be overridden to process messages before sending to the server.
    message = preSend(message);
    if (message == null) return null;
    // TODO: There probably should be an original() method here to allow refinements of CLI feature..?
    // If not null, the CLI should print to console and GUI should display in chat window
    System.out.println(message);
    // Now call postSend to allow further processing before sending to the server
    message = postSend(message);
    return message;
  }

	

	

  /**
   * ?NOTE: This CLI feature implements send() by printing to console after preSend() and before postSend().
   * This function sends a message to the server.
   * @param message the message to process and send to the server.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  private String  send__wrappee__src  (String message) {
    // Should be overridden to process messages before sending to the server.
    message = preSend(message);
    if (message == null) return null;
    // TODO: There probably should be an send__wrappee__src() method here to allow refinements of CLI feature..?
    // If not null, the CLI should print to console and GUI should display in chat window
    System.out.println(message);
    // Now call postSend to allow further processing before sending to the server
    message = postSend(message);
    return message;
  }

	

  /**
   * ?NOTE: This CLI feature implements send() by printing to console after preSend() and before postSend().
   * This function sends a message to the server.
   * @param message the message to process and send to the server.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  protected String send  (String message) {
    // Should be overridden to process messages before sending to the server.
    message = preSend(message);
    if (message == null) return null;
    // TODO: There probably should be an send__wrappee__src() method here to allow refinements of CLI feature..?
    // If not null, the CLI should print to console and GUI should display in chat window
    System.out.println(message);
    // Now call postSend to allow further processing before sending to the server
    message = postSend(message);
    return message;
  }

	

  /**
   * ?NOTE: This CLI feature implements receive() by printing to console after preReceive() and before postReceive().
   * This function should be called and can be overridden to handle incoming messages from the server.
   * @param message the message received from the server.
   * @return the processed message (can be the same as input).
   */
  protected String receive  (String message) {
    message = preReceive(message);
    if (message == null) return null;
    // If not null, the CLI should print to console and GUI should display in chat window
    System.out.println(message);
    message = postReceive(message);
    return message;
  }


}
