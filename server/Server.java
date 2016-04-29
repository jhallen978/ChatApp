//Name: Jonathan Allen
//Filename: MTServer.java
//Use: Multithreaded DNS Server for port 6052

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.*;

public class Server {

	public static final int DEFAULT_PORT = 1337;

	public static List<Connection> clientList = new ArrayList<Connection>();
	public static List<String> userList = new ArrayList<String>();

	// construct a thread pool for concurrency
	private static final Executor exec = Executors.newCachedThreadPool();

	protected static class Handler {
		public static final int BUFFER_SIZE = 2048;


		public String getServerTime() {
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE:dd:MMM:yyyy:HH:mm:ss:z", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			return dateFormat.format(calendar.getTime());
		}

		/**
		 * this method is invoked by a separate thread
		 * @throws Exception 
		 */
		public void process(Socket socket, List<Connection> clientList, Connection thisConn) throws Exception {

			try {
				//declare client streams
				PrintStream out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				try {
					while(socket!=null){
						String inputLine = in.readLine();
						String[] commands = inputLine.split(" ");
						int cmd = 0;

						//Get command number
						System.out.println("Split command: " + Arrays.toString(commands));

						//if commands[0] is number
						if(commands[0].matches("[0-9]")){
							cmd = Integer.parseInt(commands[0]);

							//if commands[0] is exit (7)	
						}else if(commands[0].matches("7/r/n")){
							String stripped = commands[0].replace("/r/n", "");
							cmd = Integer.parseInt(stripped);
						}else{
							throw new Exception("Server command is not recognized.\n");
						}


						//do command
						switch(cmd){
						
						case 3: //public msg
							System.out.println("pub msg: <3><_><message></r/n>");

							String pubMessage = "";
							int i;
							
							for(i = 1; i < commands.length; i++){
								if( (i + 1) == commands.length){
									pubMessage = pubMessage.concat(commands[i]);
								}else{
									pubMessage = pubMessage.concat(commands[i] + " ");
								}
							}

							sendToAll("5" + " "
									+ thisConn.getUsername() + " "
									+ getServerTime() + " "
									+ pubMessage);
							
							System.out.println("Response: " 
												+"5" + " "
												+ thisConn.getUsername() + " "
												+ getServerTime() + " "
												+ pubMessage);
							break;
								
						case 4: //private msg
							System.out.println("priv msg: <4><_><fromUser><_><toUser><_><message></r/n>");

							String privMessage = "";
							int i1;
							for( i1 = 3; i1 < commands.length; i1++){
								if( (i1 + 1) == commands.length){
									privMessage = privMessage.concat(commands[i1]);
								}else{
									privMessage = privMessage.concat(commands[i1] + " ");
								}
							}

							sendToOne("6" + " " 
									+ thisConn.getUsername() + " " 
									+ commands[2] + " "
									+ getServerTime() + " "
									+ privMessage
									, commands[2] );
							
							System.out.println("Response: "
									+ "6" + " " 
									+ thisConn.getUsername() + " " 
									+ commands[2] + " "
									+ getServerTime() + " "
									+ privMessage);
							break;

							//handle exit	
						case 7:
							System.out.println("exit: <7></r/n>");
							sendToOne("8/r/n", thisConn.getUsername());
							System.out.println("Resp to " + thisConn.getUsername() + ": " + "8/r/n" );
							
							clientList.remove(thisConn);
							sendToAll("9" + " " + thisConn.getUsername() + "/r/n");
							System.out.println("Resp to other clients:" + "9" + " " + thisConn.getUsername() + "/r/n" );
							
							if(in!=null)
								in.close();

							if(out!=null)
								out.close();

							if(socket!=null)
								socket.close();

							break;
						}

					}

				}catch(IOException ioe){
					System.out.println("User Logged off: " + thisConn.username);
				}catch(Exception e){
					System.out.println(e);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			if (socket != null) {
				socket.close();
			}
		}
	}

	protected static class Connection implements Runnable {
		private Socket client;
		private Handler handler = new Handler();
		private BufferedReader in;
		private PrintWriter out;
		private String username;

		public Connection(Socket client) throws IOException{
			this.client = client;
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			OutputStream jim = client.getOutputStream();
			out = new PrintWriter(jim,true);
		}

		public String getUsername(){
			return username;
		}

		public void printFail(String msg) throws IOException{
			try {
				System.out.println(msg);
				this.out.print("2/r/n");
				this.out.flush();
				this.client.close();
			}catch (IOException ioe){
				System.err.println(ioe);
			}
		}
		/**
		 * This method runs in a separate thread.
		 */
		public void run() { 
			try {
				String psocket = in.readLine();

				System.out.println("Client Socket: " + psocket);

				StringTokenizer st = new StringTokenizer(psocket);

				if (st.hasMoreElements() && st.nextToken().equalsIgnoreCase("0") && st.hasMoreElements()) {
					String[] strippedRN = st.nextToken().split("/r/n");
					
					this.username = strippedRN[0];

					if(this.username.matches("[a-zA-Z0-9_]+") && (this.username.length() <= 16)){
						if(userList.contains(this.username)){
							printFail("Same username!");
						}else{
							System.out.println("Unique User: Authed!");
							sendToAll("10" + " " + this.username + "/r/n");
							userList.add(this.username);
							clientList.add(this);
							this.out.println("1" + " " + userList.toString() + " " + "Hello Friend" + "/r/n");
							this.out.flush();
							handler.process(client, clientList, this);
						}
					}else{
						printFail("Incorrect username format! (16 max length) A-Z,a-z,0-9,_");
					}
				}else{
					printFail("Incorrect user auth format! [<0>< ><username></r/n>]");
				}
			}catch (java.io.IOException ioe) {
				System.err.println(ioe);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void write(String message){
			this.out.println(message);
			this.out.flush();			
		}
	}

	public static void sendToAll( String message){
		for( Connection client: clientList){
			client.write(message);
		}
	}

	public static void sendToOne(String message, String toUser){
		int i;
		for(i = 0; i < clientList.size(); i++){
			String findUsername;
			findUsername = clientList.get(i).getUsername();
			if(findUsername.matches(toUser)){
				clientList.get(i).write(message);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		ServerSocket sock = null;

		try {
			// establish the socket
			sock = new ServerSocket(DEFAULT_PORT);

			while (true) {
				/**
				 * now listen for connections
				 * and service the connection in a separate thread.
				 */
				Runnable task = new Connection(sock.accept());				
				exec.execute(task);
			}
		}
		catch (IOException ioe) { }
		finally {
			if (sock != null)
				sock.close();
		}
	}
}