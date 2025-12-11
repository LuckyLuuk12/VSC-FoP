import java.util.*; import java.util.Arrays; import java.io.*; 
import java.net.*; 
import java.util.concurrent.*; import java.io.BufferedWriter; 
import java.io.IOException; 
import java.io.UncheckedIOException; 
import java.nio.file.Files; 
import java.nio.file.Path; 
import java.nio.file.Paths; 
import java.nio.file.StandardOpenOption; 
import java.time.OffsetDateTime; 
import java.time.format.DateTimeFormatter; 
import java.util.concurrent.ConcurrentHashMap; 
import java.util.concurrent.ConcurrentMap; import javax.swing.*; 

import java.io.BufferedReader; 
import java.io.InputStreamReader; 
import javax.swing.border.EmptyBorder; 
import java.awt.*; 
import java.awt.event.*; 
import java.io.PrintWriter; import java.net.Socket; 
import java.util.concurrent.CountDownLatch; 
import java.util.concurrent.TimeUnit; 
import java.util.concurrent.LinkedBlockingQueue; 

public   class  Client {
	

  protected static final int ROTATION  = 13;

	


  /**
   * ?NOTE: We encrypt messages after UI-handling before sending to the server.
   * @param message the message to encrypt.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  private String  postSend__wrappee__ANSIColour  (String message) {
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

	

  private String  postSend__wrappee__Logging  (String message) {
    if (message == null) return null;

    // let lower refinements run first
    String processed = postSend__wrappee__ANSIColour(message);
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

	

  private String  postSend__wrappee__CeasarEncryption  (String message) {
    if (message == null) return null;
    // let original refinements run first, then encrypt the resulting text
    String processed = postSend__wrappee__Logging(message);
    if (processed == null) return null;
    return rotateString(processed, ROTATION);
  }

	
  
  /**
   * This function should be called and can be overridden to process messages after UI-handling but before sending to the server.
   * @param message the message to process after UI-handling but before sending to the server.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
 
  protected String postSend  (String message) {
    // Should be overridden to process messages after UI-handling but before sending to the server.
    return message;
  }

	


  /**
   * ?NOTE: We decrypt messages before UI-handling when receiving from the server.
   * @param message the message to decrypt.
   * @return the processed message (can be the same as input). If null is returned the message is not sent/displayed.
   */  
  private String  preReceive__wrappee__Logging  (String message) {
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

	

  /**
   * This function should be called and can be overridden to process messages before UI-handling.
   * @param message the message to process before UI-handling.
   * @return the processed message (can be the same as input). If null is returned the message is not sent/displayed.
   */
  protected String preReceive  (String message) {
    // Should be overridden to process messages before UI-handling.
    return message;
  }

	
  // protected field available to composition; FXColour sets it
  protected String currentColor  = null;

	

  // avoid generic diamond usage for older compiler levels
  public static final java.util.List COLOR_COMMANDS  = Arrays.asList(
    "/set-color", "/set-colour", "/setcolor", "/setcolour", "/colour", "/color"
  );

	

  private String  preSend__wrappee__ANSIColour  (String message) {
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

	

  private String  preSend__wrappee__CLI  (String message) {
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
    return preSend__wrappee__ANSIColour(message);
  }

	
  /**
   * This function should be called and can be overridden to process messages before sending them to the server.
   * @param message the message to process before sending to the server.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  protected String preSend  (String message) {
    // Should be overridden to process messages before UI-handling.
    return message;
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

	

  private String asANSI(String hexColor) {
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

	
  /**
   * !NOTE: UI features MUST refine this method to actually handle the received message!
   * This function should be called and can be overridden to handle incoming messages from the server.
   * @param message the message received from the server.
   * @return the processed message (can be the same as input).
   */
  protected String receive  (String message) {
    message = preReceive(message);
    if (message == null) return null;
    // If not null, the CLI should print to console and GUI should display in chat window
    message = postReceive(message);
    return message;
  }

	
  private void  onStart__wrappee__MessageSound(Socket socket) {
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

	
  /// In other files like the CLI / GUI we can override this method to do something when the client starts.
  protected void onStart(Socket socket) {}

	

  /**
   * ?NOTE: This CLI feature implements send() by printing to console after preSend() and before postSend().
   * This function sends a message to the server.
   * @param message the message to process and send to the server.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  private String  send__wrappee__MessageSound  (String message) {
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

	


/*
 * Everything below this is here mainly for documentation and reference.
 */


  /**
   * !NOTE: UI features MUST refine this method to actually send the message to the server!
   * This function sends a message to the server.
   * @param message the message to process and send to the server.
   * @return the processed message (can be the same as input). If null is returned the message is not sent.
   */
  protected String send  (String message) {
    // Should be overridden to process messages before sending to the server.
    message = preSend(message);
    if (message == null) return null;
    // If not null, the CLI should print to console and GUI should display in chat window
    message = postSend(message);
    return message;
  }

	
  protected static final Path LOG_DIR = Paths.get("logs");

	
  protected static final String LOG_NAME = "chat";

	
  protected static final ConcurrentMap<String, BufferedWriter> writers = new ConcurrentHashMap<String, BufferedWriter>();

	
  protected static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	

  private void  onStop__wrappee__MessageSound  (Socket socket) {
    // call refinements below/previous in the chain first
    original(socket);
    // then close all log writers
    closeAll();
  }

	
  /// In other files like the CLI / GUI we can override this method to do something when the client stops.
  protected void onStop  (Socket socket) {}

	

  private String  postReceive__wrappee__CeasarEncryption  (String message) {
    // Let other refinements process first, then log the final message
    String processed = original(message);

    try {
      BufferedWriter bw = getWriter(LOG_NAME);
      String text = processed == null ? "" : processed.replace("\n", "\\n");
      String line = String.format("%s IN  : %s%n", TS.format(OffsetDateTime.now()), text);
      synchronized (bw) {
        bw.write(line);
        bw.flush();
      }
    } catch (Exception e) {
      System.err.println("Logging (postReceive) failed: " + e.getMessage());
    }

    return processed;
  }

	
  /**
   * This function should be called and can be overridden to process messages after UI-handling.
   * @param message the message to process after UI-handling.
   * @return the processed message (can be the same as input).
   */
  protected String postReceive  (String message) {
    // Should be overridden to process messages after UI-handling.
    return message;
  }

	

  private static BufferedWriter getWriter(String name) throws IOException {
    BufferedWriter bw = writers.get(name);
    if (bw != null) return bw;

    synchronized (writers) {
      bw = writers.get(name);
      if (bw == null) {
        try {
          Files.createDirectories(LOG_DIR);
          Path p = LOG_DIR.resolve(name + ".log");
          bw = Files.newBufferedWriter(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
          writers.put(name, bw);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
    return bw;
  }

	

  private static void closeAll() {
    for (BufferedWriter bw : writers.values()) {
      try {
        synchronized (bw) { bw.close(); }
      } catch (IOException ignored) {}
    }
    writers.clear();
  }

	

  public static char rotateChar(char c, int rotation) {
    if (c >= 'a' && c <= 'z') {
      return (char) ('a' + (c - 'a' + rotation + 26) % 26);
    } else if (c >= 'A' && c <= 'Z') {
      return (char) ('A' + (c - 'A' + rotation + 26) % 26);
    } else {
      return c;
    }
  }

	

  public static String rotateString(String input, int rotation) {
    StringBuilder result = new StringBuilder();
    for (char c : input.toCharArray()) {
      result.append(rotateChar(c, rotation));
    }
    return result.toString();
  }

	

  private void playSound(String type) {
    // Use system beep as a placeholder for sound playing
    java.awt.Toolkit.getDefaultToolkit().beep();
  }

	
  private static volatile boolean swingStarted = false;

	
  private static final CountDownLatch START_LATCH = new CountDownLatch(1);

	

  // per-client send queue and sender thread
  protected final LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>();

	
  protected volatile Thread senderThread = null;

	
  protected volatile boolean senderRunning = false;

	

  // Inner Swing application and append API
  public static  class  SwingApp {
		
    private static JFrame frame;

		
    private static JPanel messagesPanel;

		
    private static JScrollPane scrollPane;

		
    private static JTextField input;

		
    private static JButton sendBtn;

		
    private static volatile SendHandler SEND_HANDLER;

		

    public static  interface  SendHandler {
			 void onSendText(String text);


		}

		
    public static void setSendHandler(SendHandler h) { SEND_HANDLER = h; }

		
    public static void publishSend(String text) {
      SendHandler h = SEND_HANDLER;
      if (h != null) h.onSendText(text);
    }

		

    public static void createAndShow() {
      frame = new JFrame("Chat GUI");
      frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      frame.setLayout(new BorderLayout());

      messagesPanel = new JPanel();
      messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
      messagesPanel.setBorder(new EmptyBorder(5,5,5,5));

      scrollPane = new JScrollPane(messagesPanel);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

      JPanel bottom = new JPanel(new BorderLayout(5,5));
      input = new JTextField();
      sendBtn = new JButton("Send");
      sendBtn.setFocusable(false);

      // send action
      ActionListener sendAction = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String text = input.getText();
          if (text == null || text.trim().length() == 0) return;
          publishSend(text);
          input.setText("");
        }
      };
      input.addActionListener(sendAction);
      sendBtn.addActionListener(sendAction);

      bottom.add(input, BorderLayout.CENTER);
      bottom.add(sendBtn, BorderLayout.EAST);

      frame.add(scrollPane, BorderLayout.CENTER);
      frame.add(bottom, BorderLayout.SOUTH);

      frame.setSize(640, 420);
      frame.setLocationRelativeTo(null);
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          // hide only; other features decide process shutdown
          frame.setVisible(false);
        }
      });

      frame.setVisible(true);
    }

		

    public static void showWindow() {
      if (frame == null) return;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() { frame.setVisible(true); }
      });
    }

		

    public static void closeWindow() {
      if (frame == null) return;
      SwingUtilities.invokeLater(new Runnable() { public void run() { frame.dispose(); frame = null; messagesPanel = null; } });
    }

		

    // Append message without style
    public static void appendMessage(final String text) {
      appendMessage(text, null);
    }

		

    /**
     * Append message with optional cssStyle string (e.g. "-fx-text-fill: #RRGGBB;").
     * If cssStyle contains a hex color it will be applied to the JLabel foreground.
     */
    public static void appendMessage(final String text, final String cssStyle) {
      final String safe = text == null ? "" : text;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (messagesPanel == null) return;
          JLabel lbl = new JLabel(safe);
          lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
          if (cssStyle != null) {
            // try to extract a hex color like #RRGGBB
            try {
              int idx = cssStyle.indexOf('#');
              if (idx >= 0 && idx + 7 <= cssStyle.length()) {
                String hex = cssStyle.substring(idx, idx + 7);
                try {
                  Color c = Color.decode(hex);
                  lbl.setForeground(c);
                } catch (NumberFormatException nfe) { /* ignore invalid color */ }
              }
            } catch (Throwable ignored) {
            	System.out.println("Ignored2: " + ignored);
            }
          }
          messagesPanel.add(lbl);
          messagesPanel.revalidate();
          // scroll to bottom
          JScrollBar vsb = scrollPane.getVerticalScrollBar();
          if (vsb != null) { vsb.setValue(vsb.getMaximum()); }
        }
      });
    }


	}

	
  public static final String SEPARATOR = " » ";

	
  /// This is here such that features can use it to separate name and message in the UI.
  
  protected Socket socket = null;

	
  protected String name = "";

	
  private final String HOST = "localhost";

	
  private final int PORT = 1234;

	

  /**
   * The client sets the isServer field to false.
   * Then it parses the arguments for clientName and because Client is a sub-feature of the server we already have the host and port as fields
   * @param args the arguments passed to the program, example: java Main <name>
   */

  /**
   * This function makes a new thread for the client and starts a socket connection to the server.
   * Calls onStart() when the connection is established.
   * @throws Exception
   */
  public void startClient() {
    try {
    	socket = new Socket(HOST, PORT);
    	System.out.println("Connected to server at " + HOST + ":" + PORT + " as " + name);
    	onStart(socket);
    } catch (Exception e) {
    	System.err.println(e);
    }
  }

	

  /**
   * This function closes the socket connection to the server.
   * @throws Exception
   */
  public void stopClient() throws Exception {
    if (socket != null && !socket.isClosed()) {
      socket.close();
      System.out.println("Disconnected from server");
    }
    onStop(socket);
  }


}
