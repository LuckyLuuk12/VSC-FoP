/**
 * Plays a sound on postSend() and postReceive().
 */
public class Client {
  protected String postSend(String message) {
    // Play sound for sent message
    playSound("send");
    return original(message);
  }

  protected String postReceive(String message) {
    // Play sound for received message
    playSound("receive");
    return original(message);
  }

  private void playSound(String type) {
    // Use system beep as a placeholder for sound playing
    java.awt.Toolkit.getDefaultToolkit().beep();
  }

}