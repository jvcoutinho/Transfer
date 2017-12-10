import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;

public class RTTCalculator implements Runnable {
	
	JLabel lblRTT;
	
	private DataOutputStream dOUT;
	private DataInputStream dIN;
	
	private long RTTSendTime;
	
	public RTTCalculator(Socket socket, JLabel lblRTT) {
		
		try {
			this.dOUT = new DataOutputStream(socket.getOutputStream());
			this.dIN = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.lblRTT = lblRTT;
		lblRTT.setVisible(true);
	}

	@Override
	public void run() {	
		
		(new Thread(new Runnable()  { // Receiver.

			@Override
			public void run() {
				
				String RTT;
				
				while(true) {
					
					try {
					
						RTT = dIN.readUTF();
						
						switch(RTT) {
						
						case "RTTr": // Response to mine.
							lblRTT.setText("RTT: " + (System.nanoTime() - RTTSendTime)/1000 + " us");
							break;
							
						case "RTTs": // Other side's RTT.
							dOUT.writeUTF("RTTr");
							break;
						}
					
					} catch (IOException e) {
						//e.printStackTrace();
					}
					
				}
				
			}
			
		})).start();
		
		(new Timer()).schedule(new TimerTask() { // Sender.

			@Override
			public void run() {
					
					try {
						dOUT.writeUTF("RTTs");
						RTTSendTime = System.nanoTime();
					} catch (IOException e) {
						//e.printStackTrace();
					}
				
			}
			
		}, 2000, 300);
		
	}
}
