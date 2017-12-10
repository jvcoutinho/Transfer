import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
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
import java.awt.SystemColor;
import javax.swing.JPopupMenu;
import java.awt.Component;
import javax.swing.DropMode;

public class Client {

	// Core variables.
	private JFrame frmClient;
	private Socket socket;
	private DataOutputStream dOUT;
	private boolean uploadPaused;
	private boolean uploadCanceled;
	private boolean connectionStablished;
	private JLabel connectionStatus;
	private JLabel lblRTT;
	private long RTTSendTime;
	private long RTTArrivalTime;
	
	// Utility variables.
	private JPanel connectionIndicator;
	private JLabel lblFileName;
	private JLabel lblSize;
	private JPanel downloadRequisitionPanel;
	private String fileName;
	private long fileSize;
	private JTextField txtfilePath;
	private boolean dOUTCreated;
	private JLabel uploadFileName;
	private JLabel uploadPercentage;
	private JLabel uploadTimeRemaining;
	private JPanel uploadPanel;
	private JProgressBar uploadProgressBar;
	private JTextField txtIpDestino;
	private JLabel lblConectarse;
	
	public Client() {
		initialize();
		uploadPaused = false;
		uploadCanceled = false;
		dOUTCreated = false;
		connectionStablished = false;
	}
	
	public void connect(String host, int port) {
		
		try {
			
			frmClient.getComponent(0).getComponentAt(12, 10).setVisible(false);
			socket = new Socket(host, port);	
			
			connectionIndicator.setBackground(new Color(0, 150, 0));
			connectionStatus.setText(socket.getInetAddress().toString());
			
			dOUT = new DataOutputStream(socket.getOutputStream());
			dOUTCreated = true;
			connectionStablished = true;
			txtIpDestino.setVisible(false);
			lblConectarse.setVisible(false);
			
			(new Thread(new InputHandler())).start();
			(new Thread(new RTTCalculator())).start();
			
			
		} catch (UnknownHostException e) {
			connectionIndicator.setBackground(new Color(150, 0, 0));
			connectionStatus.setText("Host desconhecido.");
		} catch (IOException e) {
			connectionIndicator.setBackground(new Color(150, 0, 0));
			connectionStatus.setText("Conexão falha!");
		}
	
	}

	
	private void sendFile(String filePath) {
	
		try {
			FileInputStream fIN = new FileInputStream(filePath);
			
			int fileSize = fIN.available();
			double totalSend = 0;
			int remaining = fileSize;
			byte[] fileStream = new byte[1024];
			
			dOUT.writeUTF("STAR");
			
			long initialTime = System.nanoTime(); // Getting the initial time for speed calculation.
			int send = fIN.read(fileStream);
			while(send > 0 && !uploadCanceled) {
				
				totalSend += send;
				remaining -= send;
				//System.out.printf("%.0f%%   %.2fs\n", (totalSend / fileSize) * 100, getRemainingTime(totalSend, initialTime, remaining) / 1000000);
				
				synchronized(dOUT) { // Mutual exclusion: acquire the lock.
					while(uploadPaused) {
						System.out.println("Upload pausado.");
						dOUT.wait();
					} 
						
						dOUT.writeUTF("FILE");
						dOUT.write(fileStream, 0, send);
				}
				updateUploadPanel(totalSend, fileSize, remaining, initialTime);
				
				send = fIN.read(fileStream);
			}
			
			//System.out.println("Finished.");
			uploadTimeRemaining.setText("Upload concluído.");
			fIN.close();
			
			if(uploadCanceled)
				dOUT.writeUTF("DELE");
			
		} catch (FileNotFoundException e) {
			System.out.println("Arquivo não encontrado.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Falha no socket.");
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void updateUploadPanel(double totalSend, int fileSize, int remaining, long initialTime) {
		uploadPercentage.setText(String.format("%.0f%%", 100 * totalSend/fileSize));
		uploadTimeRemaining.setText(String.format("| %.0fs restante(s)", getRemainingTime(totalSend, remaining, initialTime)));
		uploadProgressBar.setValue((int) totalSend);		
	}

	private double getRemainingTime(double totalSend, int remaining, long initialTime) {
		Long elapsedTime = System.nanoTime() - initialTime;
		double speed = totalSend / elapsedTime;
		double averageSpeed = 0;
		averageSpeed = 0.005 * averageSpeed + 0.995 * speed; // The speed used for time calculation is a weighted average.
		return averageSpeed * remaining / 1000000;
	}

	/* Initializing the contents of the frame. */
	private void initialize() {
		
		frmClient = new JFrame();
		frmClient.setResizable(false);
		frmClient.setTitle("Client");
		frmClient.setBounds(100, 100, 700, 500);
		frmClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmClient.getContentPane().setLayout(null);
		
		/* Connection status panel. */
		
		JPanel connectionPanel = new JPanel();
		connectionPanel.setBackground(Color.WHITE);
		connectionPanel.setBounds(0, 0, 700, 70);
		frmClient.getContentPane().add(connectionPanel);
		connectionPanel.setLayout(null);
		
		connectionStatus = new JLabel("Não conectado.");
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
		
		txtIpDestino = new JTextField();
		txtIpDestino.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 11));
		txtIpDestino.setText("localhost");
		txtIpDestino.setBounds(343, 18, 106, 20);
		connectionPanel.add(txtIpDestino);
		txtIpDestino.setColumns(10);
		
		lblConectarse = new JLabel("CONECTAR-SE");
		lblConectarse.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				lblConectarse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblConectarse.setCursor(Cursor.getDefaultCursor());
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if(!connectionStablished) 
					connect(txtIpDestino.getText(), 6000);
				/*else {
					try {
						dOUT.close();
						socket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					connectionStatus.setText("Não conectado.");
					connectionIndicator.setBackground(Color.red);
				
					
				}*/
				
			}
		});
		lblConectarse.setForeground(Color.BLUE);
		lblConectarse.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 11));
		lblConectarse.setBounds(459, 21, 106, 14);
		connectionPanel.add(lblConectarse);
		
		
		/* ------------------------------ */
		
		/* File selection panel. */
		JPanel fileSelectionPanel = new JPanel();
		fileSelectionPanel.setBackground(new Color(250, 250, 250));
		fileSelectionPanel.setBounds(0, 70, 694, 37);
		frmClient.getContentPane().add(fileSelectionPanel);
		fileSelectionPanel.setLayout(null);
		
		txtfilePath = new JTextField();
		txtfilePath.setBounds(10, 3, 455, 31);
		fileSelectionPanel.add(txtfilePath);
		txtfilePath.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
		txtfilePath.setText("  Use o botão ao lado para selecionar um arquivo.");
		txtfilePath.setBackground(new Color(255, 255, 255));
		txtfilePath.setEditable(false);
		txtfilePath.setColumns(10);
		
		JButton btnEnviar = new JButton("ENVIAR");
		btnEnviar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				try {
					// Sending a download requisition.
					synchronized(dOUT) {
						dOUT.writeUTF("HELO " + fileName);
						dOUT.writeLong(fileSize);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
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
				int returnVal = chooser.showOpenDialog(frmClient);
				if(returnVal == JFileChooser.APPROVE_OPTION) { // File selected successfully.
					txtfilePath.setText("  " + chooser.getSelectedFile().getPath());
					btnEnviar.setEnabled(true);
					fileName = chooser.getSelectedFile().getName();
					fileSize = chooser.getSelectedFile().length();
				}
			}
		});
		btnSelecionar.setBounds(475, 3, 117, 31);
		fileSelectionPanel.add(btnSelecionar);
		btnSelecionar.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 13));
		
		/* ------------------------------------ */
		
		/* Upload fields. */
		uploadPanel = new JPanel();
		uploadPanel.setBackground(new Color(242, 242, 242));
		uploadPanel.setBounds(0, 128, 700, 70);
		frmClient.getContentPane().add(uploadPanel);
		uploadPanel.setLayout(null);
		uploadPanel.setVisible(false);
		
		uploadFileName = new JLabel();
		uploadFileName.setBounds(10, 11, 337, 20);
		uploadFileName.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		uploadFileName.setForeground(new Color(67, 67, 67));
		uploadPanel.add(uploadFileName);
		
		uploadPercentage = new JLabel();
		uploadPercentage.setHorizontalAlignment(SwingConstants.RIGHT);
		uploadPercentage.setBounds(10, 42, 36, 17);
		uploadPercentage.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 14));
		uploadPanel.add(uploadPercentage);
		
		uploadTimeRemaining = new JLabel();
		uploadTimeRemaining.setBounds(56, 42, 178, 17);
		uploadTimeRemaining.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		uploadTimeRemaining.setForeground(new Color(67, 67, 67));
		uploadPanel.add(uploadTimeRemaining);
		
		uploadProgressBar = new JProgressBar();
		uploadProgressBar.setForeground(new Color(150, 0, 0));
		uploadProgressBar.setStringPainted(true);
		uploadProgressBar.setString("");
		uploadProgressBar.setFont(new Font("Tahoma", Font.PLAIN, 11));
		uploadProgressBar.setBounds(0, 65, 700, 5);
		uploadPanel.add(uploadProgressBar);
		uploadProgressBar.setValue(60);
		uploadProgressBar.updateUI();
		
		/* ------------------------------------ */
		
		/* Download fields. */
		downloadRequisitionPanel = new JPanel();
		downloadRequisitionPanel.setBackground(Color.LIGHT_GRAY);
		downloadRequisitionPanel.setBounds(0, 289, 694, 115);
		frmClient.getContentPane().add(downloadRequisitionPanel);
		downloadRequisitionPanel.setLayout(null);
		downloadRequisitionPanel.setVisible(false);
	
		JLabel lblAux = new JLabel("DESEJA RECEBER");
		lblAux.setForeground(Color.WHITE);
		lblAux.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 22));
		lblAux.setBounds(39, 41, 200, 29);
		downloadRequisitionPanel.add(lblAux);
		
		lblFileName = new JLabel("<nome de arquivo>" );
		lblFileName.setForeground(Color.WHITE);
		lblFileName.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 22));
		lblFileName.setBounds(232, 41, 271, 29);
		downloadRequisitionPanel.add(lblFileName);
		
		lblSize = new JLabel("tamanho");
		lblSize.setForeground(Color.WHITE);
		lblSize.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 22));
		lblSize.setBounds(572, 41, 112, 29);
		downloadRequisitionPanel.add(lblSize);
		
		JButton btnYes = new JButton("Aceitar");
		btnYes.setBounds(242, 81, 89, 23);
		downloadRequisitionPanel.add(btnYes);
		
		JButton btnNo = new JButton("Recusar");
		btnNo.setBounds(341, 81, 89, 23);
		downloadRequisitionPanel.add(btnNo);
		
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
					Client window = new Client();
					window.frmClient.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	/* Core client side threads. */
	private class InputHandler implements Runnable {
		
		private DataInputStream dIN;
		
		public InputHandler() throws IOException {
			dIN = new DataInputStream(socket.getInputStream());
		}

		@Override
		public void run() {
			String command;
			while(!socket.isClosed()) {
				
				try {
					command = dIN.readUTF();
		
					if(command.substring(0, 4).equals("HELO")) {	// Hello, I want to send a file.
						dIN.readUTF();
					} 
					
					else if(command.substring(0, 4).equals("ACKd")) { // Two events may come from this.
						
						 // Download accepted, you can upload.
						System.out.println("Accepted.");
						uploadCanceled = false;
						uploadPaused = false;
						uploadPanel.setVisible(true);
						uploadFileName.setText(fileName);
						uploadProgressBar.setMaximum((int) fileSize); 
						
						(new Thread(new Runnable() { // Download handler thread.

							@Override
							public void run() {
								sendFile(txtfilePath.getText().substring(2));
							}
							
						})).start();
						
					}
					
					else if(command.substring(0, 4).equals("NAKd")) { // Download negated.
						System.out.println("Rejected.");
					}
					
					else if(command.substring(0, 4).equals("PAUS")) { // Pause the upload!
						
							uploadPaused = !uploadPaused;
							synchronized(dOUT) { // Acquire the lock.
								if(!uploadPaused)
									dOUT.notify();
							}
					}
					
					else if(command.substring(0, 4).equals("CANC")) { // Abort transfer!
						uploadCanceled = true;
					}
					
					else if(command.substring(0, 4).equals("RTTr")) { // Response to my packet.
						RTTArrivalTime = System.nanoTime();
						//System.out.println("RTT: cheguei.");
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
					e.printStackTrace();
				}
				
			}
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
	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
}
