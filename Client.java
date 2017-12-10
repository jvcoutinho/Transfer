import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Client {

	private JFrame frmCliente;
	private Socket socket;
	private int numUploads;
	private boolean pathSelected;
	private byte[] file;
	private boolean fileTransferred;
	
	/* Important components of the frame. */
	private JLabel connectionStatus;
	private JPanel connectionFlag;
	private JLabel lblConectadoA;
	private JTextField txtPath;
	private JButton btnSelecionar;
	private JButton btnEnviar;
	private JPanel panel;
	
	/* Inter-thread communication. */
	private DataInputStream input;
	private DataOutputStream output;
	private String fileName;
	private boolean uploadAccepted;
	private boolean uploadNegated;

	/* Launch the application. */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Client window = new Client("localhost", 6000);
					window.frmCliente.setVisible(true);
				} catch (Exception e) {
					System.out.println(e);
					e.printStackTrace();
				}
			}
		});
	}

	/* Create the application. */
	public Client(String host, int port) throws UnknownHostException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		
		numUploads = 0;
		pathSelected = false;
		uploadAccepted = false;
		uploadNegated = false;
		file = new byte[4096];
		fileTransferred = false;
		
		/* Swing se assemelha ao padrão do S.O., agora. */
		  UIManager.setLookAndFeel(
		            UIManager.getSystemLookAndFeelClassName());
		initialize(host, port);
		
	}

	/* Initialize the contents of the frame. */
	private void initialize(String host, int port) {
		frmCliente = new JFrame();
		frmCliente.setResizable(false);
		frmCliente.setTitle("Cliente");
		frmCliente.setBounds(100, 100, 700, 500);
		frmCliente.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCliente.getContentPane().setLayout(null);
		
		JPanel connectionPanel = new JPanel();
		connectionPanel.setBackground(Color.WHITE);
		connectionPanel.setBounds(27, 0, 700, 70);
		frmCliente.getContentPane().add(connectionPanel);
		connectionPanel.setLayout(null);
		
		connectionStatus = new JLabel("Conexão não estabelecida.");
		connectionStatus.setBounds(10, 0, 398, 70);
		connectionPanel.add(connectionStatus);
		connectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
		connectionStatus.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 24));
		
		connectionFlag = new JPanel();
		connectionFlag.setBackground(Color.WHITE);
		connectionFlag.setBounds(0, 0, 28, 70);
		frmCliente.getContentPane().add(connectionFlag);
		
		JLabel lblConectarse = new JLabel("CONECTAR-SE");
		lblConectarse.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				try {
					
					socket = new Socket(host, port);
				//	(new Thread(new RTTCalculator(false, socket, connectionStatus))).start();
					connectionFlag.setBackground(new Color(0, 150, 0));
					connectionStatus.setText(socket.getInetAddress().toString());
					connectionStatus.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 40));
					lblConectadoA.setVisible(true);
					
					input = new DataInputStream(socket.getInputStream());
					output = new DataOutputStream(socket.getOutputStream());
					(new Thread(new InputHandler())).start();
					
				} catch (UnknownHostException e) {
					connectionError("Host desconhecido.");
				} catch (IOException e) {
					connectionError("Conexão falha.");
					lblConectadoA.setVisible(false);
				}
				
				
				//lblConectarse.setText("DESCONECTAR-SE");
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblConectarse.setForeground(new Color(0, 100, 255));
				lblConectarse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblConectarse.setForeground(new Color(0, 0, 0));
				lblConectarse.setCursor(Cursor.getDefaultCursor());
			}
		});
		
		lblConectarse.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
		lblConectarse.setBounds(570, 0, 76, 14);
		connectionPanel.add(lblConectarse);
		
		lblConectadoA = new JLabel("CONECTADO A");
		lblConectadoA.setBounds(10, 1, 81, 14);
		lblConectadoA.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
		lblConectadoA.setVisible(false);
		connectionPanel.add(lblConectadoA);
		
		txtPath = new JTextField();
		txtPath.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
		txtPath.setText("  Use o botão ao lado para selecionar um arquivo.");
		txtPath.setBackground(new Color(255, 255, 255));
		txtPath.setEditable(false);
		txtPath.setBounds(10, 73, 455, 31);
		frmCliente.getContentPane().add(txtPath);
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
			       fileName = chooser.getSelectedFile().getName();
			    } else
			    	pathSelected = false;

			}
		});
		btnSelecionar.setBounds(475, 73, 117, 31);
		frmCliente.getContentPane().add(btnSelecionar);
		btnSelecionar.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 13));
		
		btnEnviar = new JButton("ENVIAR");
		btnEnviar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			
				if(pathSelected) {
					
					// Send a request.
					try {
						output.write(1);
						output.writeUTF(fileName);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				} else {				
					txtPath.setText("  Selecione um arquivo!");
				}
			}
		});
		btnEnviar.setBounds(591, 73, 93, 31);
		frmCliente.getContentPane().add(btnEnviar);
		btnEnviar.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 13));
		
		panel = new JPanel();
		panel.setBackground(new Color(250, 250, 250));
		panel.setBounds(0, 70, 694, 37);
		frmCliente.getContentPane().add(panel);
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
						System.out.println(input.readUTF());
						break;
						
					case 2: /** Download accepted. */
						(new Thread(new UploadHandler())).start();
				
						break;
						
					case 3: /** Download rejected. */
						uploadNegated = true;	
					
						break;
						
					case 4: /** RTT's packet. */
						break;
					
					case 5: /** Data sending. */
						if(!fileTransferred)
							System.out.println("Transferindo...");
						break;
						
					}
				}
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/* Handle the upload. */
	public class UploadHandler implements Runnable {

		@Override
		public void run() {
				try {
					
					FileInputStream finput = new FileInputStream(txtPath.getText());
					byte[] file = new byte[4096];
					output.write(5);
					output.writeInt(finput.available());
					while(finput.read(file) > 0) {
					
						output.write(file);
						
					}
					fileTransferred = true;
					finput.close();
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		
		}
		
	}
	
	/* Handle the download. 
	public class DownloadHandler implements Runnable {

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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}*/
	
}
