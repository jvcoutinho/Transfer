import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JPanel;
import java.awt.Color;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTextField;

public class Server {

	private JFrame frmServidor;
	private ServerSocket serverSocket;
	private boolean isConnected;
	private int numUploads;
	private boolean pathSelected;
	private boolean fileTransferred;
	
	/* Important components of the frame. */
	private JLabel connectionStatus;
	private JPanel connectionFlag;
	private JLabel lblConectadoA;
	private JTextField txtPath;
	private JButton btnSelecionar;
	private JButton btnEnviar;
	private JPanel panel;
	private JPanel downloadRequisitionPanel;
	private JLabel lblfileName;
	
	/* Inter-thread communication. */
	private DataInputStream input;
	private DataOutputStream output;
	private boolean uploadAccepted;
	private boolean uploadNegated;

	/* Launch the application. */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Server window = new Server(6000);
					window.frmServidor.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		
		
	}

	/* Create the application. */
	public Server(int port) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		isConnected = false;
		numUploads = 0;
		pathSelected = false;
		fileTransferred = true;
		
		/* Swing se assemelha ao padrão do S.O., agora. */
		  UIManager.setLookAndFeel(
		            UIManager.getSystemLookAndFeelClassName());
		initialize(port);
		(new Thread(new Connection())).start();
	}


	/* Initialize the contents of the frame. */
	private void initialize(int port) throws IOException {
		frmServidor = new JFrame();
		frmServidor.setResizable(false);
		frmServidor.setTitle("Servidor");
		frmServidor.setBounds(400, 100, 700, 500);
		frmServidor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmServidor.getContentPane().setLayout(null);
		
		JPanel connectionPanel = new JPanel();
		connectionPanel.setBackground(Color.WHITE);
		connectionPanel.setBounds(27, 0, 700, 70);
		frmServidor.getContentPane().add(connectionPanel);
		connectionPanel.setLayout(null);
		
		connectionStatus = new JLabel("Conexão não estabelecida.");
		connectionStatus.setBounds(10, 0, 398, 70);
		connectionPanel.add(connectionStatus);
		connectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
		connectionStatus.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 24));
		
		JLabel lblPorta = new JLabel("PORTA " + port);
		lblPorta.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
		lblPorta.setBounds(570, 0, 71, 14);
		connectionPanel.add(lblPorta);
		
		lblConectadoA = new JLabel("CONECTADO A");
		lblConectadoA.setBounds(10, 1, 81, 14);
		lblConectadoA.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
		lblConectadoA.setVisible(false);
		connectionPanel.add(lblConectadoA);
		
		connectionFlag = new JPanel();
		connectionFlag.setBackground(Color.WHITE);
		connectionFlag.setBounds(0, 0, 28, 70);
		frmServidor.getContentPane().add(connectionFlag);
		
		txtPath = new JTextField();
		txtPath.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
		txtPath.setText("  Use o botão ao lado para selecionar um arquivo.");
		txtPath.setBackground(new Color(255, 255, 255));
		txtPath.setEditable(false);
		txtPath.setBounds(10, 73, 455, 31);
		frmServidor.getContentPane().add(txtPath);
		txtPath.setColumns(10);
		
		btnSelecionar = new JButton("SELECIONAR");
		btnSelecionar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				JFileChooser chooser = new JFileChooser();
			    int returnVal = chooser.showOpenDialog(null);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	pathSelected = true;
			       txtPath.setText(chooser.getSelectedFile().getPath());
			      
			    }

			}
		});
		btnSelecionar.setBounds(475, 73, 117, 31);
		frmServidor.getContentPane().add(btnSelecionar);
		btnSelecionar.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 13));
		
		btnEnviar = new JButton("ENVIAR");
		btnEnviar.setBounds(591, 73, 93, 31);
		frmServidor.getContentPane().add(btnEnviar);
		btnEnviar.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 13));
		
		panel = new JPanel();
		panel.setBackground(new Color(250, 250, 250));
		panel.setBounds(0, 70, 694, 37);
		frmServidor.getContentPane().add(panel);
		
		downloadRequisitionPanel = new JPanel();
		downloadRequisitionPanel.setBackground(Color.LIGHT_GRAY);
		downloadRequisitionPanel.setBounds(0, 356, 694, 115);
		frmServidor.getContentPane().add(downloadRequisitionPanel);
		downloadRequisitionPanel.setLayout(null);
		downloadRequisitionPanel.setVisible(false);
		
		JLabel lblNewLabel = new JLabel("DESEJA RECEBER");
		lblNewLabel.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 11));
		lblNewLabel.setBounds(39, 11, 97, 20);
		downloadRequisitionPanel.add(lblNewLabel);
		
		lblfileName = new JLabel("fileName");
		lblfileName.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 22));
		lblfileName.setBounds(39, 36, 271, 46);
		downloadRequisitionPanel.add(lblfileName);
		
		JButton btnYes = new JButton("Sim");
		btnYes.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					output.write(2);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnYes.setBounds(94, 65, 89, 23);
		downloadRequisitionPanel.add(btnYes);
		
		JButton btnNo = new JButton("Não");
		btnNo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					output.write(3);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnNo.setBounds(193, 65, 89, 23);
		downloadRequisitionPanel.add(btnNo);
		
	}
	
	public class Connection implements Runnable {
		
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(6000);
				Socket socket = serverSocket.accept();
				
				output = new DataOutputStream(socket.getOutputStream());
				input = new DataInputStream(socket.getInputStream());
				
				connectionFlag.setBackground(new Color(0, 150, 0));
				connectionStatus.setText(socket.getInetAddress().toString());
				connectionStatus.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 40));
				lblConectadoA.setVisible(true);
				
				btnEnviar.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
					
						if(pathSelected) {
							(new Thread(new UploadHandler(txtPath.getText()))).start();
						} else {				
							txtPath.setText("  Selecione um arquivo!");
						}
					}
				});
				
				(new Thread(new InputHandler())).start();
				
			//	(new Thread(new RTTCalculator(true, socket, connectionStatus))).start();
			} catch (IOException e) {
				connectionError("Conexão falha.");
				//e.printStackTrace();
			}
		
		}
	}
	
	public void connectionError(String displayError) {
		connectionFlag.setBackground(Color.RED);
		connectionStatus.setText(displayError);
	}
	
	/* Handle the data input. */
	public class InputHandler implements Runnable {

		@Override
		public void run() {
			
			try {
				
				while(true) {
					
					switch(input.read()) {
					
					case 1: /** Incoming download. */
						String fileName = input.readUTF();
						//System.out.println(input.readUTF());
						lblfileName.setText(fileName);
						downloadRequisitionPanel.setVisible(true);
						break;
						
					case 2: /** Download accepted. */
						uploadAccepted = true;
						break;
						
					case 3: /** Download rejected. */
						uploadNegated = true;
						break;
						
						
					case 4: /** RTT's packet. */
						break;
					
					case 5: /** Data sending. */
						System.out.println("chegou o 5.");
						if(!fileTransferred)
							break;
						else {
							
							int filesize = input.readInt();
							(new Thread(new DownloadHandler(filesize))).start();
							break;
						}
						
						
					}
				}
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} 
			
		}
		
	}
	
	/* Handle the upload. */
	public class UploadHandler implements Runnable {

		private String fileName;
		
		public UploadHandler (String fileName) {
			this.fileName = fileName;
		}
		
		@Override
		public void run() {
			
			
			try {
				
				// Send a request.
				output.write(1);
				output.writeUTF(fileName);
				
				// Wait for an answer.
				while(!uploadAccepted && !uploadNegated);
				
				// Accepted download: start to send.
				if(uploadAccepted) {
					System.out.println("oi");
				}
				
				// Rejected download: undo everything.
				else if(uploadNegated) {
					uploadNegated = false;
					//uploadLabel.setText("Upload negado.");
				}
					
			} catch (IOException e) {
				connectionStatus.setText("Conexão perdida.");
				//e.printStackTrace();
			}
		}
		
	}
	
	/* Handle the download. */
	public class DownloadHandler implements Runnable {

		private int filesize;
		
		public DownloadHandler (int filesize) {
			this.filesize = filesize;
		}
		
		@Override
		public void run() {
			
			
			try {
				fileTransferred = false;
				// Receive stuff.
				byte[] fileStream = new byte[4096];
				FileOutputStream fos = new FileOutputStream("oi.zip");
				
				int read = 0;
				int remaining = filesize;
				
				read = input.read(fileStream, 0, Math.min(fileStream.length, remaining));
				while(read > 0) {
					remaining -= read;
					fos.write(fileStream, 0, read);
					read = input.read(fileStream, 0, Math.min(fileStream.length, remaining));
				}
				System.out.println("terminei.");
				fileTransferred = true;
			} catch (IOException e) {
				connectionStatus.setText("Conexão perdida.");
				//e.printStackTrace();
			}
		}
		
	}
}
	
