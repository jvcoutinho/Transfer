import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class DownloadHandler implements Runnable {
	
		private Socket transferSocket;
		
		private long fileSize;
		private double totalRead;
		
		private int slot;
		private JPanel downloadPanel;
		private JLabel fileName;
		private JLabel percentage;
		private JLabel timeRemaining;
		private JProgressBar progressBar;
		
		private double averageSpeed;
		private final double SPEEDCONSTANT;
		
		private String fullPath;
		
		public DownloadHandler(Socket transferSocket, String filePath, String downloadFileName, long downloadFileSize, JPanel downloadPanel, int slot) {
			this.transferSocket = transferSocket;
			this.slot = slot;
			this.downloadPanel = downloadPanel;
			
			fileSize = downloadFileSize;
			
			progressBar = (JProgressBar) downloadPanel.getComponent(0);
			progressBar.setMaximum((int) fileSize);
			progressBar.setString("");
			fileName = (JLabel) downloadPanel.getComponent(1);
			fileName.setText(downloadFileName);
			percentage = (JLabel) downloadPanel.getComponent(2);
			timeRemaining = (JLabel) downloadPanel.getComponent(3);
			downloadPanel.setVisible(true);
			
			averageSpeed = 0;
			SPEEDCONSTANT = 0.995;
			
			fullPath = filePath + downloadFileName;
			
			Transfer.downloadCanceled[slot] = false;
		}

		@Override
		public void run() {
			
			try {
				
				DataInputStream dIN = new DataInputStream(transferSocket.getInputStream()); 
				FileOutputStream fOS = new FileOutputStream(fullPath);
				System.out.println(fullPath);
				
				byte[] fileStream = new byte[1024];
				long remaining = fileSize;
				totalRead = 0;
				
				createRemainingTimeScheduler();
				
				long initialTime = System.nanoTime();
				int read = dIN.read(fileStream, 0, (int) Math.min(remaining, fileStream.length));
				while(read > 0 && !Transfer.downloadCanceled[slot]) {
					totalRead += read;
					remaining -= read;
					
					fOS.write(fileStream, 0, read);
					updateDownloadPanel(totalRead, remaining, initialTime);	
					read = dIN.read(fileStream, 0, (int) Math.min(remaining, fileStream.length));	
				}
				
				dIN.close();
				fOS.close();
				
				
				percentage.setText("");

				if(Transfer.downloadCanceled[slot]) {
					System.out.println(new File(fullPath).delete());
					fileName.setText("");
					progressBar.setValue(0);
					timeRemaining.setText("Cancelado.");	
				} else {
					timeRemaining.setText("Finalizado.");
					Thread.sleep(3000);
					downloadPanel.setVisible(false);
				}
								
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("Finalizado.");
		}
		
		private void createRemainingTimeScheduler() {
			
			(new Timer()).schedule(new TimerTask() {
				
				double totalReadEarlier = 0;
				double speed;

				@Override
				public void run() {
					speed = totalRead - totalReadEarlier;
					averageSpeed = (SPEEDCONSTANT) * averageSpeed + (1 - SPEEDCONSTANT) * speed; // The speed used for time calculation is a weighted average.
					timeRemaining.setText(String.format("%.0f s restante(s)", (fileSize - totalRead) / (averageSpeed * 10) ));
					totalReadEarlier = totalRead;
				} 
				
				
			}, 1000, 1000);
			
		}
		
		private void updateDownloadPanel(double totalRead, long remaining, long initialTime) {
			percentage.setText(String.format("%.0f%%", 100 * totalRead/fileSize));
		//	timeRemaining.setText(String.format("%.0f s restante(s)", getRemainingTime(initialTime, totalRead, remaining)));
			progressBar.setValue((int) totalRead);
		}
		
		private double getRemainingTime(long initialTime, double totalRead, long remaining) {
			Long elapsedTime = System.nanoTime() - initialTime;
			double speed = totalRead / elapsedTime;
			averageSpeed = 0.015 * averageSpeed + 0.985 * speed; // The speed used for time calculation is a weighted average.
			return averageSpeed * remaining / 1000000;
		}

	}