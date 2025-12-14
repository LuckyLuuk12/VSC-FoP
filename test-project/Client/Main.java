public    class   Main {
	
	

  public static void main  (String[] args) {
	  //SERVER
    if (args.length > 2) {
      System.out.println("Usage: java Main <isServer> <name>");
      return;
    }
    boolean isServer = args.length == 1 && args[0].equals("--server");
    // I would have preferred return-first programming but that is not possible with client being a sub-feature of server
    // Because the main() of client would call this code and prepend it before its own code.
    if (isServer) {
      try {
    	Server server = new Server();
        server.startServer();
      } catch(Exception e) {
        e.printStackTrace();
      }
    } else {
    // Client
    // We check the arguments for name, host, port
      try {
    	Client client = new Client();
    	client.name = args[1];
    	client.startClient();
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }


}
