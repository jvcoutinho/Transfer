import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ConnectionHandler implements Runnable {

	private int port;
	private ServerSocket serverSocket;
	private Socket socket;
	private JFrame mainFrame;
	private DataInputStream dIN;
	private DataOutputStream dOUT;
	
	public ConnectionHandler(int port, DataInputStream dIN, DataOutputStream dOUT, JFrame frmTransfer) {
		this.port = port;
		mainFrame = frmTransfer;
		this.dIN = dIN;
		this.dOUT = dOUT;
	}

	@Override
	public void run() {
		
		JPanel connectionIndicator = (JPanel) mainFrame.getContentPane().getComponent(0).getComponentAt(0, 0);
		JLabel connectionStatus = (JLabel) mainFrame.getContentPane().getComponent(0).getComponentAt(10, 0);
		
		try {
			
			serverSocket = new ServerSocket(port);
			
			// Wait for a connection.
			socket = serverSocket.accept();
			
			// Establish visual effects.
			connectionIndicator.setBackground(new Color(0, 150, 0));
			connectionStatus.setText(socket.getInetAddress().toString());
			
			// Initializing the data streams.
			dIN = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			dOUT = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
		} catch (IOException e) {
			connectionIndicator.setBackground(new Color(150, 0, 0));
			connectionStatus.setText("Erro de conexão.");
		}
		
	}
	
}
