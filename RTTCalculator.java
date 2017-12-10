import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Clock;
import javax.swing.JLabel;

public class RTTCalculator implements Runnable {
	
	private boolean side;
	private Socket socket;
	private JLabel lbl;
	
	public RTTCalculator(boolean side, Socket socket, JLabel lbl) {
		this.side = side; // false = Client; true = Server.
		this.socket = socket;
		this.lbl = lbl;
	}

	@Override
	public void run() {
		
		/* Criando o indicador de RTT. */
		JLabel lblRTT = new JLabel();
		lblRTT.setBounds(570, 18, 71, 14);
		lblRTT.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
		lblRTT.setText("RTT: ");
		lbl.add(lblRTT);
		lbl.updateUI();
		
		try {
			
			if(!side) {
				
				DataOutputStream senderClient = new DataOutputStream(socket.getOutputStream());
				DataInputStream receiverClient = new DataInputStream(socket.getInputStream());
				long time;
				long packet;
				long rtt = 0;
				
				while(true) {
					senderClient.writeLong(rtt);
					time = System.currentTimeMillis();
					packet = receiverClient.readLong();
					if(packet == rtt) {
						rtt = System.currentTimeMillis() - time;
						lblRTT.setText("RTT: " + rtt + " ms");
					}
				}
			}
			
			else {
				DataOutputStream senderServer = new DataOutputStream(socket.getOutputStream());
				DataInputStream receiverServer = new DataInputStream(socket.getInputStream());
				long rtt;
				while(true) {
					rtt = receiverServer.readLong();
					lblRTT.setText("RTT: " + rtt + " ms");
					senderServer.writeLong(rtt);
				}		
			}
			
		
			
			
			
			
		} catch (IOException e) {
			
			//e.printStackTrace();
		}
		
		
		
				
				
	}
		
}


