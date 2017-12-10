import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionHandler implements Runnable {

	private ServerSocket serverSocket;
	private Socket socket;
	public ConnectionHandler(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}
	
	@Override
	public void run() {
		try {
			socket = serverSocket.accept();
			
				receiveFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void receiveFile() {
		
		try {
			
			FileOutputStream fOUT = new FileOutputStream("download.zip");
			DataInputStream dIN = new DataInputStream(socket.getInputStream());
			
			int fileSize = dIN.readInt();
			int remaining = fileSize;
			int totalRead = 0;
			byte[] fileStream = new byte[4096];
		
			int read = dIN.read(fileStream, 0, Math.min(remaining, fileStream.length));
			while(read > 0) {
				
				totalRead += read;
				remaining -= read;
				System.out.println((totalRead / fileSize) * 100 + "%");
				fOUT.write(fileStream, 0, read);
				
				read = dIN.read(fileStream, 0, Math.min(remaining, fileStream.length));
			}
			
			fOUT.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

}
