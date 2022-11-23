import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Client {
	public static Logger logger; 

	public static void main(String[] args) {
		SystemTime time = new SystemTime();
		Logger logger = Logger.getLogger(Client.class.getName());
		logger.info("CLIENT IS UPPON TO START" +  time.getTime());
		
		ClockThread clockThread = new ClockThread(time);
		clockThread.start();

		SendRequest sendRequest = new SendRequest(time, "127.0.0.1", 8000);
		sendRequest.start();

		ReceiveRequest ReceiveRequest = new ReceiveRequest(time,"127.0.0.1", 8001);
		ReceiveRequest.start();
		while (true){
			try {
				Thread.sleep(1000);
			} 
			catch (InterruptedException e){
				logger.warning("INTERRUPT CLIENT IN TIME: " +  time.getTime());
				logger.warning("ERROR MESSAGE: " +  e.getMessage());
				e.printStackTrace();
			}
			logger.info("CLIENT CURRENT TIME:" +  time.getTime());
		}
	} 
	
}

class SendRequest extends Thread {
	public static Logger logger; 
	
	int temperature;
	String host;
	int port;
	SystemTime time;
	Random random = new Random();

	SendRequest(SystemTime time, String host, int port) {
		this.time = time;
		this.host = host;
		this.port = port;
		this.temperature = random.nextInt(100);
	}

	public void run() {
		Logger logger = Logger.getLogger(Client.class.getName());
		logger.info("CALLING START CLIENT.SEND_message" +  time.getTime());
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.warning("INTERRUPT CLIENT IN TIME: " +  time.getTime());
				logger.warning("ERROR MESSAGE: " +  e.getMessage());
				e.printStackTrace();
			}

			ResquestMessage request = new ResquestMessage(Integer.toString(this.temperature) + '|' + Double.toString(this.time.getTime()));
			
			try {
				Socket socket = new Socket(this.host, this.port);
				PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
				out.println(request.message);
				out.close();
				socket.close();
			} catch (Exception e) {
				logger.warning("INTERRUPT CLIENT IN TIME: " +  time.getTime());
				logger.warning("ERROR MESSAGE: " +  e.getMessage());
			}
			logger.info("TEMPERATURE: " + this.temperature );
			logger.info("REQUEST.MESSAGE: " + request.message );
			logger.info("REQUEST.ERRORMESSAGE: " + request.errorMessage );
			logger.info("REQUEST.HASERROR: " + request.hasError );
		}
	}
}

class SystemTime {
	private double time;

	public SystemTime() {
		this.time = System.currentTimeMillis()/1000.0;
	}

	public double getTime() {
		return time;
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	public void addTime(double seconds_to_add) {
		this.time += seconds_to_add;
	}
}

class ClockThread extends Thread {
	public static Logger logger; 
	SystemTime time;
	Boolean clockEnabled;
	Double clock_time = 0.0;
	Double clock_gap = 0.2; 
	long clock_step = 2; 
	Random random = new Random();

	ClockThread(SystemTime time) {
		this.time = time;
		this.clock_step = (long) (clock_gap * 1000.0);
		this.clockEnabled = true; 
	}

	public void run() {
		Logger logger = Logger.getLogger(ClockThread.class.getName());
		logger.info("CALLING START CLIENT.CLOCK" +  time.getTime());
		while (this.clockEnabled) {
			
			try {
				Thread.sleep(this.clock_step);
			} 			
			catch (InterruptedException e) {
				logger.warning("ERRORMESSAGE: " + e.getMessage() );
				e.printStackTrace();
			}
			
			time.addTime(clock_gap);
			this.clock_time += clock_gap;

			if (this.clock_time > 10.0 && random.nextInt(3) == 0){
				this.clock_time = 0.0;
				if (random.nextInt(5) == 0){	
					time.addTime(5.0);
					logger.info("GAP: +5");
				}else{
					time.addTime(-5.0);
					logger.info("GAP: -5");
				}
			}
		}
	}
}

class ReceiveRequest extends Thread {
	public static Logger logger; 
	
	String host;
	int port;
	SystemTime time;
	boolean keep_alive;

	ReceiveRequest(SystemTime time, String host, int port) {
		this.time = time;
		this.host = host;
		this.port = port;
		this.keep_alive = true;
	}

	public void run() {
		Logger logger = Logger.getLogger(Client.class.getName());
		logger.info("CALLING START CLIENT.RECEIVEREQUEST" +  this.time.getTime());
		
		BufferedReader bufferReader;
		ReceivedMessage receivedMessage = new ReceivedMessage("");

		try {
			
			ServerSocket server = new ServerSocket(port);			
			
			while (this.keep_alive) {
				
				Socket client = server.accept();
				logger.info("CONNECTING WITH: " + client.getLocalSocketAddress());
				bufferReader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
				receivedMessage.message = bufferReader.readLine().trim();
				logger.info("RECEIVED MESSAGE UNCLEARED: " + receivedMessage.message);

				if (receivedMessage.message.equals("REQUEST_TIME")) {
					
					receivedMessage.message = Double.toString(this.time.getTime());
					PrintWriter outputStream = new PrintWriter(client.getOutputStream(),true);
					
					logger.info("SEND MESSAGE CURRENT_TIME: " + receivedMessage.message);
					
					outputStream.println(receivedMessage.message);
					bufferReader.close();
					outputStream.close();
					client.close();

				}
				if(receivedMessage.message.contains(".")){
					logger.info("RECEIVED SERVER MESSAGE CURRENT_TIME: " + receivedMessage.message);
					bufferReader.close();
					this.time.setTime(Double.parseDouble(receivedMessage.message));
				} else {
					receivedMessage.errorMessage = "UNEXPECTED REQUEST MESSAGE" + receivedMessage.message;
					receivedMessage.hasError = true;
					logger.warning("ERRORMESSAGE: " + "UNEXPECTED REQUEST MESSAGE" + receivedMessage.message);
				}

				client.close();
			}
			server.close();

		} catch (IOException e) {
			receivedMessage.errorMessage = e.getMessage();
			receivedMessage.hasError = true;
			logger.warning("ERRORMESSAGE: " + e.getMessage() );
		}
		logger.info("REQUEST.MESSAGE: " + receivedMessage.message );
		logger.info("REQUEST.ERRORMESSAGE: " + receivedMessage.errorMessage );
		logger.info("REQUEST.HASERROR: " + receivedMessage.hasError );
		
	}
}

class  ResquestMessage {
	public String message;
	public String errorMessage;
	Boolean hasError;

	public ResquestMessage(String message) {
		this.message = message;
		this.errorMessage = "";
		this.hasError = false;
	}
	public ResquestMessage(String message, String errorMessage, Boolean hasError) {
		this.message = message;
		this.errorMessage = errorMessage;
		this.hasError = hasError;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setErrorMessage(String message) {
		this.errorMessage = message;
	}
	
	public void setError(Boolean hasError) {
		this.hasError = hasError;
	}
}

class  ReceivedMessage {
	public String message;
	public String errorMessage;
	Boolean hasError;

	public ReceivedMessage(String message) {
		this.message = message;
		this.errorMessage = "";
		this.hasError = false;
	}
	public ReceivedMessage(String message, String errorMessage, Boolean hasError) {
		this.message = message;
		this.errorMessage = errorMessage;
		this.hasError = hasError;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setErrorMessage(String message) {
		this.errorMessage = message;
	}
	
	public void setError(Boolean hasError) {
		this.hasError = hasError;
	}
}