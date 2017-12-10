import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.MatteBorder;

public class Server {

	// Core variables.
	private JFrame frmServer;
	private ServerSocket serverSocket;
	private Socket socket;
	private DataInputStream dIN; 
	private DataOutputStream dOUT; 
	private byte[] fileStream;
	private long remaining;
	private FileOutputStream fOUT;
	private boolean pausedDownload;
	private JLabel lblRTT;
	private Long RTTSendTime;
	private Long RTTArrivalTime;
	
	
	// Utility variables.
	private JPanel connectionIndicator;
	private JLabel connectionStatus;
	private JLabel lblFileName;
	private JPanel downloadRequisitionPanel;
	private JPanel downloadPanel;
	private JLabel lblFileSize;
	private JLabel downloadFileName;
	private JLabel downloadPercentage;
	private JLabel downloadTimeRemaining;
	private JProgressBar downloadProgressBar;
	private boolean dOUTCreated;
	
	public Server() {
		initialize();
		fileStream = new byte[1024];
		pausedDownload = false;
		dOUTCreated = false;
	}
	
	public void handleConnection() {
		
		// The server side connection handler thread.
		(new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					
					serverSocket = new ServerSocket(6000);
					socket = serverSocket.accept();
					
					connectionIndicator.setBackground(new Color(0, 150, 0));
					connectionStatus.setText(socket.getInetAddress().toString());
					
					
					dIN = new DataInputStream(socket.getInputStream());
					dOUT = new DataOutputStream(socket.getOutputStream());
					dOUTCreated = true;
					
					(new Thread(new InputHandler())).start();
					(new Thread(new RTTCalculator())).start();
					
					
				} catch (IOException e) {
					connectionIndicator.setBackground(new Color(150, 0, 0));
					connectionStatus.setText("Conexão falha!");
				
				}
				
			}			
		})).start();
	}
	
	public void receiveFile(DataInputStream dIN) {
		
		try {
			
			FileOutputStream fOUT = new FileOutputStream("download.zip");
			
			int fileSize = dIN.readInt();
			int remaining = fileSize;
			double totalRead = 0;
			byte[] fileStream = new byte[4096];
			
			long initialTime = System.nanoTime(); // Getting the initial time for speed calculation.	
			int read = dIN.read(fileStream, 0, Math.min(remaining, fileStream.length));
			while(read > 0) {
				
				totalRead += read;
				remaining -= read;
				System.out.printf("%.0f%%  %.2fs\n", (totalRead / fileSize) * 100, getRemainingTime(totalRead, initialTime, remaining) / 1000000);
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

	private double getRemainingTime(double totalRead, long initialTime, int remaining) {
		Long elapsedTime = System.nanoTime() - initialTime;
		double speed = totalRead / elapsedTime;
		return speed * remaining;
	}
	

	/* Initializing the contents of the frame. */
	private void initialize() {
		
		frmServer = new JFrame();
		frmServer.setTitle("Server");
		frmServer.setResizable(false);
		frmServer.setBounds(100, 100, 700, 500);
		frmServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmServer.getContentPane().setLayout(null);
		
		/* Connection status panel. */
	
		JPanel connectionPanel = new JPanel();
		connectionPanel.setBackground(Color.WHITE);
		connectionPanel.setBounds(0, 0, 700, 70);
		frmServer.getContentPane().add(connectionPanel);
		connectionPanel.setLayout(null);
		
		connectionStatus = new JLabel("N\u00E3o conectado.");
		connectionStatus.setBounds(10, 0, 398, 70);
		connectionPanel.add(connectionStatus);
		connectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
		connectionStatus.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 40));
		
		connectionIndicator = new JPanel();
		connectionIndicator.setBounds(0, 0, 700, 10);
		connectionPanel.add(connectionIndicator);
		
		lblRTT = new JLabel("RTT:");
		lblRTT.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 11));
		lblRTT.setBounds(587, 21, 66, 14);
		connectionPanel.add(lblRTT);
		
		/* ------------------------------ */
		
		/* File selection panel. */
		JPanel fileSelectionPanel = new JPanel();
		fileSelectionPanel.setBackground(new Color(250, 250, 250));
		fileSelectionPanel.setBounds(0, 70, 694, 37);
		frmServer.getContentPane().add(fileSelectionPanel);
		fileSelectionPanel.setLayout(null);
		
		JTextField txtfilePath = new JTextField();
		txtfilePath.setBounds(10, 3, 455, 31);
		fileSelectionPanel.add(txtfilePath);
		txtfilePath.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
		txtfilePath.setText("  Use o bot\u00E3o ao lado para selecionar um arquivo.");
		txtfilePath.setBackground(new Color(255, 255, 255));
		txtfilePath.setEditable(false);
		txtfilePath.setColumns(10);
		
		JButton btnEnviar = new JButton("ENVIAR");
		btnEnviar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//sendFile(txtfilePath.getText().substring(2));
			}

		});
		btnEnviar.setBounds(591, 3, 93, 31);
		fileSelectionPanel.add(btnEnviar);
		btnEnviar.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 13));
		btnEnviar.setEnabled(false);
		
		JButton btnSelecionar = new JButton("SELECIONAR");
		btnSelecionar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				JFileChooser chooser = new JFileChooser();
				int returnVal = chooser.showOpenDialog(frmServer);
				if(returnVal == JFileChooser.APPROVE_OPTION) { // File selected successfully.
					txtfilePath.setText("  " + chooser.getSelectedFile().getPath());
					btnEnviar.setEnabled(true);
				}
			}
		});
		btnSelecionar.setBounds(475, 3, 117, 31);
		fileSelectionPanel.add(btnSelecionar);
		btnSelecionar.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 13));
		
		/* ------------------------------------ */
		
		/* Download fields. */
		downloadRequisitionPanel = new JPanel();
		downloadRequisitionPanel.setBorder(new MatteBorder(2, 2, 0, 2, (Color) new Color(0, 120, 215)));
		downloadRequisitionPanel.setBackground(new Color(242, 242, 242));
		downloadRequisitionPanel.setBounds(1, 435, 692, 37);
		frmServer.getContentPane().add(downloadRequisitionPanel);
		downloadRequisitionPanel.setLayout(null);
		downloadRequisitionPanel.setVisible(false);
		
		lblFileName = new JLabel("Deseja baixar <filename>" );
		lblFileName.setForeground(new Color(67, 67, 67));
		lblFileName.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		lblFileName.setBounds(34, 4, 355, 29);
		downloadRequisitionPanel.add(lblFileName);
		
		JButton btnYes = new JButton("Iniciar");
		btnYes.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				try {
					synchronized(dOUT) {
						dOUT.writeUTF("ACKd");
					}
				} catch (IOException e1) {
					
					e1.printStackTrace();
				}
				
			}
		});
		btnYes.setBounds(469, 8, 89, 23);
		downloadRequisitionPanel.add(btnYes);
		
		JButton btnNo = new JButton("Recusar");
		btnNo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				try {
					synchronized(dOUT) {
						dOUT.writeUTF("NAKd");
					}
					downloadRequisitionPanel.setVisible(false);
				} catch (IOException e1) {
					
					e1.printStackTrace();
				}
				
			}
		});
		btnNo.setBounds(568, 8, 89, 23);
		downloadRequisitionPanel.add(btnNo);
		
		lblFileSize = new JLabel("(<filesize>)");
		lblFileSize.setForeground(new Color(67, 67, 67));
		lblFileSize.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		lblFileSize.setBounds(399, 4, 60, 29);
		downloadRequisitionPanel.add(lblFileSize);
		
		downloadPanel = new JPanel();
		downloadPanel.setBackground(new Color(242, 242, 242));
		downloadPanel.setBounds(0, 328, 700, 70);
		frmServer.getContentPane().add(downloadPanel);
		downloadPanel.setLayout(null);
		downloadPanel.setVisible(false);
		
		downloadFileName = new JLabel();
		downloadFileName.setBounds(10, 11, 337, 20);
		downloadFileName.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		downloadFileName.setForeground(new Color(67, 67, 67));
		downloadPanel.add(downloadFileName);
		
		downloadPercentage = new JLabel();
		downloadPercentage.setHorizontalAlignment(SwingConstants.RIGHT);
		downloadPercentage.setBounds(10, 42, 36, 17);
		downloadPercentage.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 14));
		downloadPanel.add(downloadPercentage);
		
		downloadTimeRemaining = new JLabel();
		downloadTimeRemaining.setBounds(56, 42, 178, 17);
		downloadTimeRemaining.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		downloadTimeRemaining.setForeground(new Color(67, 67, 67));
		downloadPanel.add(downloadTimeRemaining);
		
		JButton btnRestart = new JButton("Reiniciar");
		btnRestart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				//btnRestart.setEnabled(false);
				btnRestart.setVisible(false);
				try {
					synchronized(dOUT) {
						dOUT.writeUTF("ACKd");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
		});
		btnRestart.setEnabled(false);
		btnRestart.setBounds(374, 25, 89, 23);
		downloadPanel.add(btnRestart);
		btnRestart.setVisible(false);
		
		JButton btnPause = new JButton("Pausar");
		btnPause.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
			
					pausedDownload = !pausedDownload;
					try {
						synchronized(dOUT) {
							dOUT.writeUTF("PAUS");
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					if(pausedDownload) 
						btnPause.setText("Retomar");
					else 
						btnPause.setText("Pausar");
				
			}
		});
		btnPause.setBounds(374, 25, 89, 23);
		downloadPanel.add(btnPause);
		
		JButton btnCancel = new JButton("Cancelar");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				try {
					synchronized(dOUT) {
						dOUT.writeUTF("CANC");
					}
					System.out.println(downloadFileName.getText());
					(new File(downloadFileName.getText())).delete();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				downloadFileName.setText("Download cancelado.");
				btnRestart.setEnabled(true);
				btnRestart.setVisible(true);		
				
			}
		});
		btnCancel.setBounds(473, 25, 89, 23);
		downloadPanel.add(btnCancel);
		
		downloadProgressBar = new JProgressBar();
		downloadProgressBar.setStringPainted(true);
		downloadProgressBar.setString("");
		downloadProgressBar.setFont(new Font("Tahoma", Font.PLAIN, 11));
		downloadProgressBar.setBounds(0, 65, 700, 5);
		downloadPanel.add(downloadProgressBar);
		downloadProgressBar.setValue(60);
		downloadProgressBar.updateUI();
		
	
		
		
		
		/* ---------------------------- */
		
	}
	
	public static void main(String[] args) {
		
		/* Swing looks like the S.O., now. */
		  try {
			  
			UIManager.setLookAndFeel(
			            UIManager.getSystemLookAndFeelClassName());
		
		  } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Server window = new Server();
					window.frmServer.setVisible(true);
					window.handleConnection();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	/* Core server side threads. */
	private class InputHandler implements Runnable {
		
		private String fileName;
		private long fileSize;
		private double totalRead;
		private long initialTime;
		private double averageSpeed;

		@Override
		public void run() {
			String command;
			while(!socket.isClosed()) {
	
				try {
					
					command = dIN.readUTF();
			//		System.out.println(command);

					if(command.substring(0, 4).equals("HELO")) { // Hello, I want to send a file.
						fileName = command.substring(5); // Here it's name...
						fileSize = dIN.readLong(); // ... and it's size.
						
					//	System.out.println(fileName + " " + fileSize);
						printDownloadRequisitionPanel();
					
						averageSpeed = 0;
					}
					
					else if(command.substring(0, 4).equals("STAR")) { // Transfer started: preparations.
						fOUT = new FileOutputStream(fileName);
						downloadRequisitionPanel.setVisible(false);
						downloadPanel.setVisible(true);
						totalRead = 0;
						printDownloadPanel();
					//	System.out.println("Download started.");
						
						remaining = fileSize;
						downloadProgressBar.setMaximum((int) fileSize);
					//	System.out.println("Total of " + remaining);
						initialTime = System.nanoTime();
					}
					
					else if(command.substring(0, 4).equals("FILE")) { // Piece of file incoming.
					//	System.out.println("File incoming.");
						int read = dIN.read(fileStream, 0, (int) Math.min(remaining, fileStream.length));
					//	System.out.println(read + " bytes read.");
						totalRead += read;
						remaining -= read;
					//	System.out.println(remaining + " remaining.");
						updateDownloadPanel();
						fOUT.write(fileStream, 0, read);
						if(remaining == 0) { // The transfer has ended. See you later!
							System.out.println("finished.");
							fOUT.close();
							downloadTimeRemaining.setText("| Download concluído.");
						}
					}
					
					else if(command.substring(0, 4).equals("DELE")) { // I've canceled the download and the sender has accepted it.
						fOUT.close();  // This packet can be ignored if the file has already been transfered, with no problems.
					}
					
					else if(command.substring(0, 4).equals("RTTr")) { // Response to my packet.
						RTTArrivalTime = System.nanoTime();
						lblRTT.setText("RTT: " + (RTTArrivalTime - RTTSendTime) / 1000000 + " MS");
					}
					
					else if(command.substring(0, 4).equals("RTTs")) { // I'm the receiver, now.
						synchronized(dOUT) { 
							try {
								dOUT.writeUTF("RTTr");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					
				} catch (IOException e) {
					connectionIndicator.setBackground(new Color(150, 0, 0));
					connectionStatus.setText("Conexão perdida!");
					e.printStackTrace();
				}
				
			}
		}

		private void updateDownloadPanel() {
			downloadPercentage.setText(String.format("%.0f%%", 100 * totalRead/fileSize));
			downloadTimeRemaining.setText(String.format("| %.0fs restante(s)", getRemainingTime()));
			downloadProgressBar.setValue((int) totalRead);
		}
		
		private double getRemainingTime() {
			Long elapsedTime = System.nanoTime() - initialTime;
			double speed = totalRead / elapsedTime;
			averageSpeed = 0.005 * averageSpeed + 0.995 * speed; // The speed used for time calculation is a weighted average.
			return averageSpeed * remaining / 1000000;
		}

		private void printDownloadPanel() {
			downloadFileName.setText(fileName);
			downloadPercentage.setText("0%");
			downloadTimeRemaining.setText("0s");			
		}

		private void printDownloadRequisitionPanel() {
			lblFileName.setText("Deseja receber " + fileName + '?');
			lblFileSize.setText("(" + getReducedFileSize() + "B)");
			//lblSize.setText(new Integer(fileSize/1000000).toString()+"MB");
			downloadRequisitionPanel.setVisible(true);			
		}

		private String getReducedFileSize() {
			if(fileSize >= 1000000000) // GB
				return String.format("%.1f", fileSize/(double)1000000000) + " G";
			else if(fileSize >= 1000000) // MB
				return String.format("%.1f", fileSize/(double)1000000) + " M";
			else if(fileSize >= 1000) // KB
				return String.format("%.1f", fileSize/(double)1000) + " K";
			
			return "" + fileSize;
		}
	}
	
	private class RTTCalculator implements Runnable {
		
		@Override
		public void run() {
		
			while(!dOUTCreated);
			
			(new Timer()).schedule(new TimerTask() {

				@Override
				public void run() {
					synchronized(dOUT) { // Mutual exclusion: we don't want RTT to appear in the middle of FILE and the file bytes, right?
						
						try {
							dOUT.writeUTF("RTTs");
							RTTSendTime = System.nanoTime();
						} catch (IOException e) {
							e.printStackTrace();
						}
					
					}
				}
				
			}, 2000, 100);
		}
	}
}
