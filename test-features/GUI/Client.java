import javax.swing.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Swing GUI feature fragment: lifecycle and UI only.
 * - refine onStart/onStop/send/receive
 * - provide SwingApp.appendMessage(text) and appendMessage(text, css) for other features to call
 * No color/state logic here.
 */
public class Client {
  private static volatile boolean swingStarted = false;
  private static final CountDownLatch START_LATCH = new CountDownLatch(1);

  // per-client send queue and sender thread
  protected final LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>();
  protected volatile Thread senderThread = null;
  protected volatile boolean senderRunning = false;

  // Called when the composed client starts (socket available)
  protected void onStart(final Socket socket) {
    // call lower refinements first
    original(socket);

    synchronized (Main.class) {
      if (!swingStarted) {
        swingStarted = true;
        // start Swing UI on EDT
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            try {
              SwingApp.createAndShow();
            } finally {
              START_LATCH.countDown();
            }
          }
        });
        try { START_LATCH.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
      } else {
        SwingApp.showWindow();
      }
    }
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
 
    // install send handler to delegate to this.send(...)
    final Client self = this;
    SwingApp.setSendHandler(new SwingApp.SendHandler() {
      public void onSendText(String text) {
        try { self.send(text); } catch (Throwable t) { /* swallow to avoid UI crash */ }
      }
    });

    // start sender thread that performs actual socket writes (similar to CLI writer thread)
    senderRunning = true;
    senderThread = new Thread(new Runnable() {
      public void run() {
        PrintWriter out = null;
        try {
          out = new PrintWriter(socket.getOutputStream(), true);
          while (senderRunning) {
            String toSend = null;
            try {
              toSend = sendQueue.take();
            } catch (InterruptedException ie) {
              // interrupted -> loop and check running flag
              continue;
            }
            if (toSend == null) continue;
            out.println(toSend);
          }
        } catch (IOException e) {
          System.err.println("GUI sender thread error: " + e.getMessage());
        } finally {
          if (out != null) out.close();
        }
      }
    }, "gui-sender");
    senderThread.setDaemon(true);
    senderThread.start();
  }

  // Called when the composed client stops
  protected void onStop(final Socket socket) {
    // allow refinements first
    original(socket);

    // stop sender thread
    senderRunning = false;
    if (senderThread != null) {
      senderThread.interrupt();
      try { senderThread.join(2000); } catch (InterruptedException ignored) {}
      senderThread = null;
    }
    // clear queue
    sendQueue.clear();

    try { SwingApp.closeWindow(); } catch (Throwable ignored) {}
  }

  /**
   * UI entry: send user-typed text.
   * This method is refineable by other features. It:
   *  - composes full payload (name + SEPARATOR)
   *  - runs preSend/postSend pipeline
   *  - enqueues final wire string for the sender thread to write to socket
   *  - appends the message locally (plain display, coloration handled by FXColour/FX equivalent feature)
   */
  protected String send(String text) {
    if (text == null) return null;

    String combined = text;
    try {
      combined = (this.name == null ? "" : this.name) + SEPARATOR + text;
    } catch (Throwable ignored) { /* name/SEPARATOR may not be present in some compositions */ }

    String processed = null;
    try { processed = preSend(combined); } catch (Throwable ignored) { processed = combined; }
    if (processed == null) return null;
    String toWire = null;
    try { toWire = postSend(processed); } catch (Throwable ignored) { toWire = processed; }
    if (toWire == null) return null;

    // DO NOT call original(text) here — UI is responsible for invoking pre/post pipeline in refinements.

    // enqueue for sender thread to write to socket
    try {
      sendQueue.put(toWire);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return null;
    }

    // local plain display (color feature may also call appendMessage with style for colored display)
    try { SwingApp.appendMessage(combined); } catch (Throwable ignored) {}
    return toWire;
  }

  /**
   * UI receive hook: called when a message arrives (lower-level code should call this).
   * - note: GUI is UI-level entry point; features that refine receive should handle preReceive/postReceive themselves.
   */
  protected String receive(String message) {
    if (message == null) return null;
    message = preReceive(message);

    // append plain text (colored display may have been handled by color feature)
    try { SwingApp.appendMessage(message); } catch (Throwable ignored) {
    	System.out.println("Ignored: " + ignored);
    }
    message = postReceive(message);
    return message;
  }

  // Inner Swing application and append API
  public static class SwingApp {
    private static JFrame frame;
    private static JPanel messagesPanel;
    private static JScrollPane scrollPane;
    private static JTextField input;
    private static JButton sendBtn;
    private static volatile SendHandler SEND_HANDLER;

    public static interface SendHandler { void onSendText(String text); }
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
}