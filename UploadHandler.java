import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class UploadHandler implements Runnable {
	
	private Socket transferSocket;

	private String filePath;
	private long fileSize;
	
	private double totalRead;
	
	private int slot;
	private JPanel uploadPanel;
	private JLabel fileName;
	private JLabel percentage;
	private JLabel timeRemaining;
	private JProgressBar progressBar;
	private JLabel pause;
	private JLabel cancel;
	
	private double averageSpeed;
	private final double SPEEDCONSTANT;
	
	public UploadHandler(Socket transferSocket, String filePath, String uploadFileName, long uploadFileSize, JPanel uploadPanel, int slot) {
		this.transferSocket = transferSocket;
		this.filePath = filePath;
		this.slot = slot;
		this.uploadPanel = uploadPanel;
		
		fileSize = uploadFileSize;
		
		progressBar = (JProgressBar) uploadPanel.getComponent(0);
		progressBar.setMaximum((int) fileSize);
		progressBar.setString("");
		fileName = (JLabel) uploadPanel.getComponent(1);
		fileName.setText(uploadFileName);
		percentage = (JLabel) uploadPanel.getComponent(2);
		timeRemaining = (JLabel) uploadPanel.getComponent(3);
		pause = (JLabel) uploadPanel.getComponent(4);
		pause.setVisible(true);
		cancel = (JLabel) uploadPanel.getComponent(6);
		cancel.setVisible(true);
		uploadPanel.setVisible(true);
		
		averageSpeed = 0;
		SPEEDCONSTANT = 0.995;
		
	}
	
	@Override
	public void run() {
		
		try {
			
			DataOutputStream dOUT = new DataOutputStream(transferSocket.getOutputStream());			
			FileInputStream fIN = new FileInputStream(filePath);
			
			byte[] fileStream = new byte[1024];
			long remaining = fileSize;
			totalRead = 0;
			
			createRemainingTimeScheduler();
			
			long initialTime = System.nanoTime();
			int read = fIN.read(fileStream);
			while(read > 0) {
				
				totalRead += read;
				remaining -= read;
				
				while(Transfer.transferPaused[slot]) {
					Thread.sleep(1000);
				//	initialTime = System.nanoTime();
				}				
				
				dOUT.write(fileStream, 0, read);
				updateUploadPanel(totalRead, remaining, initialTime);
				
				read = fIN.read(fileStream);
			}
			
			fIN.close();
			percentage.setText("");
			timeRemaining.setText("Finalizado.");
			pause.setVisible(false);
			cancel.setVisible(false);
			Transfer.numTransferences--;
			Thread.sleep(3000);
			uploadPanel.setVisible(false);
			
			
		} catch (IOException e) {
			fileName.setText("");
			percentage.setText("");
			timeRemaining.setText("Cancelado.");
			progressBar.setValue(0);
			Transfer.numTransferences--;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}			
	}

	private void createRemainingTimeScheduler() {
		
		(new Timer()).schedule(new TimerTask() {
			
			double totalReadEarlier = 0;
			double speed;

			@Override
			public void run() {
				speed = totalRead - totalReadEarlier;
				averageSpeed = (SPEEDCONSTANT) * averageSpeed + (1 - SPEEDCONSTANT) * speed; // The speed used for time calculation is a weighted average.
				timeRemaining.setText(String.format("%.0f s restante(s)", (fileSize - totalRead) / (speed * 10) ));
				totalReadEarlier = totalRead;
			} 
			
			
		}, 1000, 1000);
		
	}

	private void updateUploadPanel(double totalRead, long remaining, long initialTime) {
		percentage.setText(String.format("%.0f%%", 100 * totalRead/fileSize));
	//	timeRemaining.setText(String.format("%.0f s restante(s)", getRemainingTime(initialTime, totalRead, remaining)));
		progressBar.setValue((int) totalRead);		
	}
	
	private double getRemainingTime(long initialTime, double totalRead, long remaining) {
		Long elapsedTime = System.nanoTime() - initialTime;
		double speed = totalRead / elapsedTime;
		averageSpeed = (1 - SPEEDCONSTANT) * averageSpeed + SPEEDCONSTANT * speed; // The speed used for time calculation is a weighted average.
		return remaining / averageSpeed  / 1000000000;
	}

}
