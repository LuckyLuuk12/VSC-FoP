import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Client {
  protected static final Path LOG_DIR = Paths.get("logs");
  protected static final String LOG_NAME = "chat";
  protected static final ConcurrentMap<String, BufferedWriter> writers = new ConcurrentHashMap<String, BufferedWriter>();
  protected static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  protected void onStop(Socket socket) {
    // call refinements below/previous in the chain first
    original(socket);
    // then close all log writers
    closeAll();
  }

  protected String preSend(String message) {
    // Log the message before sending
    try {
      BufferedWriter bw = getWriter(LOG_NAME);
      String text = message == null ? "" : message.replace("\n", "\\n");
      String line = String.format("%s OUT : %s%n", TS.format(OffsetDateTime.now()), text);
      synchronized (bw) {
        bw.write(line);
        bw.flush();
      }
    } catch (Exception e) {
      System.err.println("Logging (preSend) failed: " + e.getMessage());
    }

    // allow refinements / original behavior to run after logging
    return original(message);
  }

  protected String postReceive(String message) {
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
}