import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class InputHandler implements Runnable {

	private ServerSocket serverSocket;
	private boolean client; // Client is the program that started the connection.
	
	private DataInputStream dIN;
	private DataOutputStream dOUT;
	
	private JFrame mainFrame;
	
	private long[] uploadFileSizes;
	private String[] uploadFileNames;
	private String[] uploadFilePaths;
	private long[] downloadFileSizes;
	private String[] downloadFileNames;
	private String[] downloadFilePaths;

	// Sender.
	public InputHandler(JFrame frmTransfer) { 
		this.dIN = Transfer.getDataInputStream();
		this.dOUT = Transfer.getDataOutputStream();
		mainFrame = frmTransfer;	
		client = true;
		
		uploadFileSizes = new long[3];
		uploadFileNames = new String[3];
		uploadFilePaths = new String[3];
		downloadFileSizes = new long[3];
		downloadFileNames = new String[3];
		downloadFilePaths = new String[3];
	}
	
	// Receiver.
	public InputHandler(JFrame frmTransfer, ServerSocket serverSocket) { 
		this.serverSocket = serverSocket;
		this.dIN = Transfer.getDataInputStream();
		this.dOUT = Transfer.getDataOutputStream();
		mainFrame = frmTransfer;
		client = false;
		
		uploadFileSizes = new long[3];
		uploadFileNames = new String[3];
		uploadFilePaths = new String[3];
		downloadFileSizes = new long[3];
		downloadFileNames = new String[3];
		downloadFilePaths = new String[3];
	}
	
	@Override
	public void run() {
		
		JPanel connectionIndicator = (JPanel) mainFrame.getContentPane().getComponent(0).getComponentAt(0, 0);
		JLabel connectionStatus = (JLabel) mainFrame.getContentPane().getComponent(0).getComponentAt(10, 0);
		String fullCommand, command;
		
		String downloadFileName = null;
		long downloadFileSize = 0;
		
		int numTransference;
		int slot;
		int nextSlot;

		while(true) {
			
			try {
				
				fullCommand = dIN.readUTF();
				command = fullCommand.substring(0, 4);
				
				switch(command) {
				
				case "HELO": // Sender: I want to send a file.
					downloadFileName = fullCommand.substring(5);
					downloadFileSize = dIN.readLong();
					showDownloadRequisitionPanel(downloadFileName, downloadFileSize);
					break;
					
				case "SEND": // Receiver: Fine, do it.
				
					slot = fullCommand.charAt(5) - 48;
					if(slot == 3) {
						nextSlot = getFreeSlotNumber(true);
						uploadFileSizes[nextSlot] = Transfer.getUploadFileSize();
						uploadFileNames[nextSlot] = Transfer.getUploadFileName();
						uploadFilePaths[nextSlot] = Transfer.getUploadFilePath();
					} else
						nextSlot = slot;
					Transfer.numTransferences++;
				
					dOUT.writeUTF("STAR " + slot);
					dOUT.flush();
					
					(new Thread(new UploadHandler(getSocket(), uploadFilePaths[nextSlot], uploadFileNames[nextSlot], uploadFileSizes[nextSlot], (JPanel) mainFrame.getContentPane().getComponent(3 + nextSlot), nextSlot))).start();
					break;
					
				case "STAR": // Sender: Be ready, I'm starting.
					
					slot = fullCommand.charAt(5) - 48;
					if(slot == 3) {
						nextSlot = getFreeSlotNumber(false);
						downloadFileSizes[nextSlot] = downloadFileSize;
						downloadFileNames[nextSlot] = downloadFileName;
						downloadFilePaths[nextSlot] = Transfer.getDownloadFilePath();
					} else
						nextSlot = slot;
					
					(new Thread(new DownloadHandler(getSocket(), downloadFilePaths[nextSlot], downloadFileNames[nextSlot], downloadFileSizes[nextSlot], (JPanel) mainFrame.getContentPane().getComponent(6 + nextSlot), nextSlot))).start();
					break;
					
				case "STOP": // Sender: The transfer's been interrupted.
					numTransference = fullCommand.charAt(5) - 48;
					Transfer.downloadCanceled[numTransference] = true;
					break;
					
				case "REST": // Sender: The transfer's been restarted.
					numTransference = fullCommand.charAt(5) - 48;
					Transfer.downloadCanceled[numTransference] = false;
					dOUT.writeUTF("SEND " + numTransference);
					dOUT.flush();
					break;
					
				}
				
			} catch (IOException e) {
				connectionIndicator.setBackground(new Color(150, 0, 0));
				connectionStatus.setText("Conexão perdida!");
				//e.printStackTrace();
			}
			
		}
		
	}

	private int getFreeSlotNumber(boolean sender) {
		if(!sender) {		
			if(!mainFrame.getContentPane().getComponent(6).isVisible() || Transfer.downloadCanceled[0])
				return 0;
			else if(!mainFrame.getContentPane().getComponent(7).isVisible() || Transfer.downloadCanceled[1])
				return 1;
			else
				return 2;
		} else {
			if(!mainFrame.getContentPane().getComponent(3).isVisible() || Transfer.uploadCanceled[0])
				return 0;
			else if(!mainFrame.getContentPane().getComponent(4).isVisible() || Transfer.uploadCanceled[1])
				return 1;
			else
				return 2;
		}
	}

	private Socket getSocket() throws UnknownHostException, IOException {
		if(client) {
			String host = ((JTextField) mainFrame.getContentPane().getComponent(0).getComponentAt(593, 18).getComponentAt(0, 1)).getText();
			return new Socket(host, 6000);
		} else {
			return serverSocket.accept();
		}
	}

	private void showDownloadRequisitionPanel(String downloadFileName, long downloadFileSize) {
		JPanel downloadRequisitionPanel = (JPanel) mainFrame.getContentPane().getComponent(2);
		((JLabel) downloadRequisitionPanel.getComponentAt(33, 21)).setText("Deseja receber " + downloadFileName);
		((JLabel) downloadRequisitionPanel.getComponentAt(546, 21)).setText("(" + getReducedFileSize(downloadFileSize) + "B)");
		downloadRequisitionPanel.setVisible(true);
		
	}

	private String getReducedFileSize(long fileSize) {
		if(fileSize >= 1000000000) // GB
			return String.format("%.1f", fileSize/(double)1000000000) + " G";
		else if(fileSize >= 1000000) // MB
			return String.format("%.1f", fileSize/(double)1000000) + " M";
		else if(fileSize >= 1000) // KB
			return String.format("%.1f", fileSize/(double)1000) + " K";
		
		return "" + fileSize;
	}	
}
