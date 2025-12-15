/**
 * Plays a sound on postSend() and postReceive().
 */
public   class  Client {
	
	
  private String  postSend__wrappee__src  (String message) {
    // Play sound for sent message
    playSound("send");
    return original(message);
  }

	
  protected String postSend  (String message) {
    // Play sound for sent message
    playSound("send");
    return postSend__wrappee__src(message);
  }

	

	

  private String  postReceive__wrappee__src  (String message) {
    // Play sound for received message
    playSound("receive");
    return original(message);
  }

	

  protected String postReceive  (String message) {
    // Play sound for received message
    playSound("receive");
    return postReceive__wrappee__src(message);
  }

	

  private void playSound  (String type) {
    // Use system beep as a placeholder for sound playing
    java.awt.Toolkit.getDefaultToolkit().beep();
  }


}
