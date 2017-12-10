import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.JTextPane;
import javax.swing.border.MatteBorder;
import javax.swing.JProgressBar;
import java.awt.Cursor;
import java.awt.SystemColor;

public class Transfer {

	private JFrame frmTransfer;
	private static DataInputStream dIN;
	private static DataOutputStream dOUT;
	
	private static String uploadFileName;
	private static long uploadFileSize;
	private static String uploadFilePath;	
	
	private static String downloadFilePath;
	
	public static int numTransferences;
	
	public static boolean[] transferPaused;
	public static boolean[] uploadCanceled;
	public static boolean[] downloadCanceled;
	
	public Transfer() {
		initialize();
		handleConnection();

		numTransferences = 0;
		transferPaused = new boolean[3];
		uploadCanceled = new boolean[3];
		downloadCanceled = new boolean[3];
	}
	
	public static DataInputStream getDataInputStream() {
		return dIN;
	}
	
	public static DataOutputStream getDataOutputStream() {
		return dOUT;
	}
	
	public static String getUploadFilePath() {
		return uploadFilePath;
	}
	
	public static String getDownloadFilePath() {
		if(downloadFilePath != "")
			return downloadFilePath + "\\";
		return downloadFilePath;
	}
	
	public static String getUploadFileName() {
		return uploadFileName;
	}
	
	public static long getUploadFileSize() {
		return uploadFileSize;
	}

	private void initialize() {
		
		/** FRAME */
		frmTransfer = new JFrame();
		frmTransfer.setResizable(false);
		frmTransfer.setTitle("Transfer");
		frmTransfer.setBounds(100, 100, 1000, 500);
		frmTransfer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmTransfer.getContentPane().setLayout(null);
		JPanel connectionPanel = new JPanel();
		connectionPanel.setBackground(Color.WHITE);
		connectionPanel.setBounds(0, 0, 1000, 70);
		frmTransfer.getContentPane().add(connectionPanel);
		connectionPanel.setLayout(null);
		
		JLabel connectionStatus = new JLabel("N\u00E3o conectado.");
		connectionStatus.setBounds(10, 0, 398, 70);
		connectionPanel.add(connectionStatus);
		connectionStatus.setHorizontalAlignment(SwingConstants.LEFT);
		connectionStatus.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 40));
		
		JPanel connectionIndicator = new JPanel();
		connectionIndicator.setBounds(0, 0, 1000, 10);
		connectionPanel.add(connectionIndicator);
		
		JLabel lblRTT = new JLabel("RTT:");
		lblRTT.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 11));
		lblRTT.setBounds(887, 21, 66, 14);
		lblRTT.setVisible(false);
		connectionPanel.add(lblRTT);
		
		JPanel connectPanel = new JPanel();
		connectPanel.setBounds(593, 18, 273, 23);
		connectionPanel.add(connectPanel);
		connectPanel.setLayout(null);
		
		JTextField txtIPHost = new JTextField();
		txtIPHost.setBounds(0, 1, 150, 21);
		connectPanel.add(txtIPHost);
		txtIPHost.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 11));
		txtIPHost.setText("localhost");
		txtIPHost.setColumns(10);
		
		JButton btnConnect = new JButton("CONECTAR-SE");
		btnConnect.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				connect(txtIPHost.getText(), 6000);
			}
		});
		btnConnect.setBounds(149, 0, 124, 23);
		connectPanel.add(btnConnect);
		btnConnect.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 11));
		/*******************************************************************/
		
		/** FILE SELECTION */
		JPanel fileSelectionPanel = new JPanel();
		fileSelectionPanel.setBorder(new MatteBorder(0, 0, 1, 0, (Color) new Color(200, 200, 200)));
		fileSelectionPanel.setBounds(10, 66, 970, 46);
		frmTransfer.getContentPane().add(fileSelectionPanel);
		fileSelectionPanel.setLayout(null);
		
		JTextPane txtpnFile = new JTextPane();
		txtpnFile.setBounds(0, 16, 665, 23);
		fileSelectionPanel.add(txtpnFile);
		txtpnFile.setFont(new Font("Lucida Sans", Font.PLAIN, 13));
		txtpnFile.setText("Selecione um arquivo para enviar.");
		txtpnFile.setEnabled(false);
		txtpnFile.setEditable(false);
		
		JButton btnEnviar = new JButton("ENVIAR");
		btnEnviar.setBounds(875, 16, 89, 23);
		fileSelectionPanel.add(btnEnviar);
		btnEnviar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				
				if(numTransferences == 3) 
					txtpnFile.setText("Máximo de 3 transferências simultâneas!");
				else {
					try {
						
						dOUT.writeUTF("HELO " + uploadFileName); // I want to send a file.
						dOUT.writeLong(uploadFileSize);
						dOUT.flush();
						
						txtpnFile.setText("Selecione um arquivo para enviar.");
						btnEnviar.setEnabled(false);
					} catch (NullPointerException e1) {
						txtpnFile.setText("Conecte-se!");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					
				}
			}
		});
		btnEnviar.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnEnviar.setEnabled(false);
		
		JButton btnSelecionarArquivo = new JButton("SELECIONAR ARQUIVO");
		btnSelecionarArquivo.setBounds(699, 16, 166, 23);
		fileSelectionPanel.add(btnSelecionarArquivo);
		btnSelecionarArquivo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				JFileChooser chooser = new JFileChooser();
				if(chooser.showOpenDialog(frmTransfer) == JFileChooser.APPROVE_OPTION) {
					uploadFilePath = chooser.getSelectedFile().getPath();
					uploadFileName = chooser.getSelectedFile().getName();
					uploadFileSize = chooser.getSelectedFile().length();
					txtpnFile.setText(uploadFilePath);
					btnEnviar.setEnabled(true);
				}
				
			}
		});
		btnSelecionarArquivo.setFont(new Font("Tahoma", Font.BOLD, 11));
		/*******************************************************************/
		
		/** DOWNLOAD REQUISITION */
		JPanel downloadRequisitionPanel = new JPanel();
		downloadRequisitionPanel.setBorder(new MatteBorder(2, 2, 0, 2, (Color) new Color(0, 120, 215)));
		downloadRequisitionPanel.setBounds(35, 407, 924, 64);
		frmTransfer.getContentPane().add(downloadRequisitionPanel);
		downloadRequisitionPanel.setLayout(null);
		downloadRequisitionPanel.setVisible(false);
		
		JLabel lblFileName = new JLabel("Deseja baixar <filename>" );
		lblFileName.setForeground(new Color(67, 67, 67));
		lblFileName.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
		lblFileName.setBounds(33, 21, 475, 22);
		downloadRequisitionPanel.add(lblFileName);
		
		JLabel lblFileSize = new JLabel("(<filesize>)");
		lblFileSize.setHorizontalAlignment(SwingConstants.RIGHT);
		lblFileSize.setForeground(new Color(67, 67, 67));
		lblFileSize.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		lblFileSize.setBounds(529, 21, 77, 23);
		downloadRequisitionPanel.add(lblFileSize);
		
		JButton btnSalvar = new JButton("Salvar");
		btnSalvar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				downloadFilePath = "";
				synchronized(dOUT) {
					try {
						dOUT.writeUTF("SEND 3");
						dOUT.flush();
						downloadRequisitionPanel.setVisible(false);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		btnSalvar.setBounds(618, 21, 89, 23);
		downloadRequisitionPanel.add(btnSalvar);
		
		JButton btnSalvarComo = new JButton("Salvar como");
		btnSalvarComo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if(chooser.showOpenDialog(frmTransfer) == JFileChooser.APPROVE_OPTION) {
					downloadFilePath = chooser.getSelectedFile().toString();
					synchronized(dOUT) {
						try {
							dOUT.writeUTF("SEND 3");
							dOUT.flush();
							downloadRequisitionPanel.setVisible(false);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}				
			}
		});
		btnSalvarComo.setBounds(717, 21, 98, 23);
		downloadRequisitionPanel.add(btnSalvarComo);
		
		JButton btnRejeitar = new JButton("Rejeitar");
		btnRejeitar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				downloadRequisitionPanel.setVisible(false);
			}
		});
		btnRejeitar.setBounds(825, 21, 89, 23);
		downloadRequisitionPanel.add(btnRejeitar);
		/*******************************************************************/
		
		/** UPLOAD PANELS */		
		JPanel uploadPanel = new JPanel();
		uploadPanel.setBounds(10, 134, 470, 70);
		frmTransfer.getContentPane().add(uploadPanel);
		uploadPanel.setLayout(null);
		uploadPanel.setVisible(false);
		
		JProgressBar uploadProgressBar = new JProgressBar();
		uploadProgressBar.setStringPainted(true);
		uploadProgressBar.setBounds(0, 63, 470, 7);
		uploadProgressBar.setForeground(new Color(150, 0, 0));
		uploadPanel.add(uploadProgressBar);
		
		JLabel lblUploadFileName = new JLabel("<nomedoarquivo>");
		lblUploadFileName.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
		lblUploadFileName.setBounds(10,0, 360, 28);
		uploadPanel.add(lblUploadFileName);
		
		JLabel uploadPercentage = new JLabel("100%");
		uploadPercentage.setHorizontalAlignment(SwingConstants.RIGHT);
		uploadPercentage.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 15));
		uploadPercentage.setBounds(412, 0, 48, 28);
		uploadPanel.add(uploadPercentage);
		
		JLabel uploadTimeRemaining = new JLabel("100s restantes");
		uploadTimeRemaining.setForeground(new Color(87, 87, 87));
		uploadTimeRemaining.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		uploadTimeRemaining.setBounds(10, 39, 125, 14);
		uploadPanel.add(uploadTimeRemaining);
		
		JLabel lblPausarUpload = new JLabel("Pausar");
		lblPausarUpload.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblPausarUpload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblPausarUpload.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblPausarUpload.setCursor(Cursor.getDefaultCursor());
				lblPausarUpload.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				transferPaused[0] = !transferPaused[0];
				
				if(transferPaused[0]) {
					lblPausarUpload.setText("Retomar");
					uploadTimeRemaining.setText("Pausado.");
				} else {
					lblPausarUpload.setText("Pausar");
					uploadTimeRemaining.setText("Retomando...");
				}
			}
		});
		lblPausarUpload.setHorizontalAlignment(SwingConstants.RIGHT);
		lblPausarUpload.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblPausarUpload.setForeground(new Color(0, 78, 140));
		lblPausarUpload.setBounds(332, 39, 59, 14);
		uploadPanel.add(lblPausarUpload);
		
		JLabel lblReiniciarUpload = new JLabel("Reiniciar");
		lblReiniciarUpload.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblReiniciarUpload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblReiniciarUpload.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblReiniciarUpload.setCursor(Cursor.getDefaultCursor());
				lblReiniciarUpload.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				uploadCanceled[0] = false;
				
				synchronized (dOUT) {
					try {
						dOUT.writeUTF("REST 0");
						dOUT.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				lblReiniciarUpload.setVisible(false);
			}
		});
		lblReiniciarUpload.setHorizontalAlignment(SwingConstants.RIGHT);
		lblReiniciarUpload.setForeground(new Color(0, 78, 140));
		lblReiniciarUpload.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblReiniciarUpload.setBounds(401, 39, 59, 14);
		lblReiniciarUpload.setVisible(false);
		uploadPanel.add(lblReiniciarUpload);
		
		JLabel lblCancelarUpload = new JLabel("Cancelar");
		lblCancelarUpload.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblCancelarUpload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblCancelarUpload.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblCancelarUpload.setCursor(Cursor.getDefaultCursor());
				lblCancelarUpload.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				uploadCanceled[0] = true;
				
				synchronized (dOUT) {
					try {
						dOUT.writeUTF("STOP 0");
						dOUT.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				lblPausarUpload.setVisible(false);
				lblCancelarUpload.setVisible(false);
				lblReiniciarUpload.setVisible(true);
			}
		});
		lblCancelarUpload.setHorizontalAlignment(SwingConstants.RIGHT);
		lblCancelarUpload.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblCancelarUpload.setForeground(new Color(0, 78, 140));
		lblCancelarUpload.setBounds(401, 39, 59, 14);
		uploadPanel.add(lblCancelarUpload);
		
		/* **********************************/
		
		JPanel uploadPanel2 = new JPanel();
		uploadPanel2.setBounds(10, 230, 470, 70);
		frmTransfer.getContentPane().add(uploadPanel2);
		uploadPanel2.setLayout(null);
		uploadPanel2.setVisible(false);
		
		JProgressBar uploadProgressBar2 = new JProgressBar();
		uploadProgressBar2.setStringPainted(true);
		uploadProgressBar2.setForeground(new Color(150, 0, 0));
		uploadProgressBar2.setBounds(0, 63, 470, 7);
		uploadPanel2.add(uploadProgressBar2);
		
		JLabel lblUploadFileName2 = new JLabel("<nomedoarquivo>");
		lblUploadFileName2.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
		lblUploadFileName2.setBounds(10, 0, 360, 28);
		uploadPanel2.add(lblUploadFileName2);
		
		JLabel uploadPercentage2 = new JLabel("100%");
		uploadPercentage2.setHorizontalAlignment(SwingConstants.RIGHT);
		uploadPercentage2.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 15));
		uploadPercentage2.setBounds(412, 0, 48, 28);
		uploadPanel2.add(uploadPercentage2);
		
		JLabel uploadTimeRemaining2 = new JLabel("100s restantes");
		uploadTimeRemaining2.setForeground(new Color(87, 87, 87));
		uploadTimeRemaining2.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		uploadTimeRemaining2.setBounds(10, 39, 125, 14);
		uploadPanel2.add(uploadTimeRemaining2);
		
		JLabel lblPausarUpload2 = new JLabel("Pausar");
		lblPausarUpload2.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblPausarUpload2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblPausarUpload2.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblPausarUpload2.setCursor(Cursor.getDefaultCursor());
				lblPausarUpload2.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				transferPaused[1] = !transferPaused[1];
				
				if(transferPaused[1]) {
					lblPausarUpload2.setText("Retomar");
					uploadTimeRemaining2.setText("Pausado.");
				} else {
					lblPausarUpload2.setText("Pausar");
					uploadTimeRemaining2.setText("Retomando...");
				}
			}
		});
		lblPausarUpload2.setHorizontalAlignment(SwingConstants.RIGHT);
		lblPausarUpload2.setForeground(new Color(0, 78, 140));
		lblPausarUpload2.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblPausarUpload2.setBounds(332, 39, 59, 14);
		uploadPanel2.add(lblPausarUpload2);
		
		JLabel lblReiniciarUpload2 = new JLabel("Reiniciar");
		lblReiniciarUpload2.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblReiniciarUpload2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblReiniciarUpload2.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblReiniciarUpload2.setCursor(Cursor.getDefaultCursor());
				lblReiniciarUpload2.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				uploadCanceled[1] = false;
				
				synchronized (dOUT) {
					try {
						dOUT.writeUTF("REST 1");
						dOUT.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				lblReiniciarUpload2.setVisible(false);
			}
		});
		lblReiniciarUpload2.setHorizontalAlignment(SwingConstants.RIGHT);
		lblReiniciarUpload2.setForeground(new Color(0, 78, 140));
		lblReiniciarUpload2.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblReiniciarUpload2.setBounds(401, 39, 59, 14);
		lblReiniciarUpload2.setVisible(false);
		uploadPanel2.add(lblReiniciarUpload2);
		
		JLabel lblCancelarUpload2 = new JLabel("Cancelar");
		lblCancelarUpload2.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblCancelarUpload2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblCancelarUpload2.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblCancelarUpload2.setCursor(Cursor.getDefaultCursor());
				lblCancelarUpload2.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				uploadCanceled[1] = true;
				
				synchronized (dOUT) {
					try {
						dOUT.writeUTF("STOP 1");
						dOUT.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				lblPausarUpload2.setVisible(false);
				lblCancelarUpload2.setVisible(false);
				lblReiniciarUpload2.setVisible(true);
			}
		});
		lblCancelarUpload2.setHorizontalAlignment(SwingConstants.RIGHT);
		lblCancelarUpload2.setForeground(new Color(0, 78, 140));
		lblCancelarUpload2.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblCancelarUpload2.setBounds(401, 39, 59, 14);
		uploadPanel2.add(lblCancelarUpload2);
		
		/* **********************************/
		
		JPanel uploadPanel3 = new JPanel();
		uploadPanel3.setLayout(null);
		uploadPanel3.setBounds(10, 326, 470, 70);
		frmTransfer.getContentPane().add(uploadPanel3);
		uploadPanel3.setVisible(false);
		
		JProgressBar uploadProgressBar3 = new JProgressBar();
		uploadProgressBar3.setStringPainted(true);
		uploadProgressBar3.setForeground(new Color(150, 0, 0));
		uploadProgressBar3.setBounds(0, 63, 470, 7);
		uploadPanel3.add(uploadProgressBar3);
		
		JLabel lblUploadFileName3 = new JLabel("<nomedoarquivo>");
		lblUploadFileName3.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
		lblUploadFileName3.setBounds(10, 0, 360, 28);
		uploadPanel3.add(lblUploadFileName3);
		
		JLabel uploadPercentage3 = new JLabel("100%");
		uploadPercentage3.setHorizontalAlignment(SwingConstants.RIGHT);
		uploadPercentage3.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 15));
		uploadPercentage3.setBounds(412, 0, 48, 28);
		uploadPanel3.add(uploadPercentage3);
		
		JLabel uploadTimeRemaining3 = new JLabel("100s restantes");
		uploadTimeRemaining3.setForeground(new Color(87, 87, 87));
		uploadTimeRemaining3.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		uploadTimeRemaining3.setBounds(10, 39, 125, 14);
		uploadPanel3.add(uploadTimeRemaining3);
		
		JLabel lblPausarUpload3 = new JLabel("Pausar");
		lblPausarUpload3.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblPausarUpload3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblPausarUpload3.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblPausarUpload3.setCursor(Cursor.getDefaultCursor());
				lblPausarUpload3.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				transferPaused[2] = !transferPaused[2];
				
				if(transferPaused[2]) {
					lblPausarUpload3.setText("Retomar");
					uploadTimeRemaining3.setText("Pausado.");
				} else {
					lblPausarUpload3.setText("Pausar");
					uploadTimeRemaining3.setText("Retomando...");
				}
			}
		});
		lblPausarUpload3.setHorizontalAlignment(SwingConstants.RIGHT);
		lblPausarUpload3.setForeground(new Color(0, 78, 140));
		lblPausarUpload3.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblPausarUpload3.setBounds(332, 39, 59, 14);
		uploadPanel3.add(lblPausarUpload3);
		
		JLabel lblReiniciarUpload3 = new JLabel("Reiniciar");
		lblReiniciarUpload3.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblReiniciarUpload3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblReiniciarUpload3.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblReiniciarUpload3.setCursor(Cursor.getDefaultCursor());
				lblReiniciarUpload3.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				uploadCanceled[2] = false;
				
				synchronized (dOUT) {
					try {
						dOUT.writeUTF("REST 2");
						dOUT.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				lblReiniciarUpload3.setVisible(false);
			}
		});
		lblReiniciarUpload3.setHorizontalAlignment(SwingConstants.RIGHT);
		lblReiniciarUpload3.setForeground(new Color(0, 78, 140));
		lblReiniciarUpload3.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblReiniciarUpload3.setBounds(401, 39, 59, 14);
		lblReiniciarUpload3.setVisible(false);
		uploadPanel3.add(lblReiniciarUpload3);
		
		JLabel lblCancelarUpload3 = new JLabel("Cancelar");
		lblCancelarUpload3.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(MouseEvent e) {
				lblCancelarUpload3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lblCancelarUpload3.setForeground(new Color(105, 105, 105));
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				lblCancelarUpload3.setCursor(Cursor.getDefaultCursor());
				lblCancelarUpload3.setForeground(new Color(0, 78, 140));
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				uploadCanceled[2] = true;
				
				synchronized (dOUT) {
					try {
						dOUT.writeUTF("STOP 2");
						dOUT.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				lblPausarUpload3.setVisible(false);
				lblCancelarUpload3.setVisible(false);
				lblReiniciarUpload3.setVisible(true);
			}
		});
		lblCancelarUpload3.setHorizontalAlignment(SwingConstants.RIGHT);
		lblCancelarUpload3.setForeground(new Color(0, 78, 140));
		lblCancelarUpload3.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblCancelarUpload3.setBounds(401, 39, 59, 14);
		uploadPanel3.add(lblCancelarUpload3);
		
		/*******************************************************************/
		
		/** DOWNLOAD PANELS */	
		JPanel downloadPanel = new JPanel();
		downloadPanel.setLayout(null);
		downloadPanel.setBounds(510, 134, 470, 70);
		frmTransfer.getContentPane().add(downloadPanel);
		downloadPanel.setVisible(false);
		
		JProgressBar downloadProgressBar = new JProgressBar();
		downloadProgressBar.setStringPainted(true);
		downloadProgressBar.setBounds(0, 63, 470, 7);
		downloadProgressBar.setForeground(new Color(0, 120, 215));
		downloadPanel.add(downloadProgressBar);
		
		JLabel lblDownloadFileName = new JLabel("<nomedoarquivo>");
		lblDownloadFileName.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
		lblDownloadFileName.setBounds(10, 0, 360, 28);
		downloadPanel.add(lblDownloadFileName);
		
		JLabel downloadPercentage = new JLabel("100%");
		downloadPercentage.setHorizontalAlignment(SwingConstants.RIGHT);
		downloadPercentage.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 15));
		downloadPercentage.setBounds(412, 0, 48, 28);
		downloadPanel.add(downloadPercentage);
		
		JLabel downloadTimeRemaining = new JLabel("100s restantes");
		downloadTimeRemaining.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		downloadTimeRemaining.setForeground(new Color(87, 87, 87));
		downloadTimeRemaining.setBounds(10, 39, 125, 14);
		downloadPanel.add(downloadTimeRemaining);
		
		/* **********************************/
		
		JPanel downloadPanel2 = new JPanel();
		downloadPanel2.setBounds(510, 230, 470, 70);
		frmTransfer.getContentPane().add(downloadPanel2);
		downloadPanel2.setLayout(null);
		downloadPanel2.setVisible(false);
		
		JProgressBar downloadProgressBar2 = new JProgressBar();
		downloadProgressBar2.setStringPainted(true);
		downloadProgressBar2.setForeground(SystemColor.textHighlight);
		downloadProgressBar2.setBounds(0, 63, 470, 7);
		downloadPanel2.add(downloadProgressBar2);
		
		JLabel lblDownloadFileName2 = new JLabel("<nomedoarquivo>");
		lblDownloadFileName2.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
		lblDownloadFileName2.setBounds(10, 0, 360, 28);
		downloadPanel2.add(lblDownloadFileName2);
		
		JLabel downloadPercentage2 = new JLabel("100%");
		downloadPercentage2.setHorizontalAlignment(SwingConstants.RIGHT);
		downloadPercentage2.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 15));
		downloadPercentage2.setBounds(412, 0, 48, 28);
		downloadPanel2.add(downloadPercentage2);
		
		JLabel downloadTimeRemaining2 = new JLabel("100s restantes");
		downloadTimeRemaining2.setForeground(new Color(87, 87, 87));
		downloadTimeRemaining2.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		downloadTimeRemaining2.setBounds(10, 39, 125, 14);
		downloadPanel2.add(downloadTimeRemaining2);
		
		/* **********************************/
		
		JPanel downloadPanel3 = new JPanel();
		downloadPanel3.setLayout(null);
		downloadPanel3.setBounds(510, 326, 470, 70);
		frmTransfer.getContentPane().add(downloadPanel3);
		downloadPanel3.setVisible(false);
		
		JProgressBar downloadProgressBar3 = new JProgressBar();
		downloadProgressBar3.setStringPainted(true);
		downloadProgressBar3.setForeground(SystemColor.textHighlight);
		downloadProgressBar3.setBounds(0, 63, 470, 7);
		downloadPanel3.add(downloadProgressBar3);
		
		JLabel lblDownloadFileName3 = new JLabel("<nomedoarquivo>");
		lblDownloadFileName3.setFont(new Font("Malgun Gothic", Font.PLAIN, 15));
		lblDownloadFileName3.setBounds(10, 0, 360, 28);
		downloadPanel3.add(lblDownloadFileName3);
		
		JLabel downloadPercentage3 = new JLabel("100%");
		downloadPercentage3.setHorizontalAlignment(SwingConstants.RIGHT);
		downloadPercentage3.setFont(new Font("Malgun Gothic Semilight", Font.BOLD, 15));
		downloadPercentage3.setBounds(412, 0, 48, 28);
		downloadPanel3.add(downloadPercentage3);
		
		JLabel downloadTimeRemaining3 = new JLabel("100s restantes");
		downloadTimeRemaining3.setForeground(new Color(87, 87, 87));
		downloadTimeRemaining3.setFont(new Font("Malgun Gothic Semilight", Font.PLAIN, 14));
		downloadTimeRemaining3.setBounds(10, 39, 125, 14);
		downloadPanel3.add(downloadTimeRemaining3);
		/*******************************************************************/
		
		/** ENVIROMNENT */	
		JPanel border = new JPanel();
		border.setBorder(new MatteBorder(0, 1, 0, 0, (Color) new Color(200, 200, 200)));
		border.setBounds(495, 134, 1, 262);
		frmTransfer.getContentPane().add(border);
		
		
	}

	private void connect(String host, int port) {
		
		JPanel connectionIndicator = (JPanel) frmTransfer.getContentPane().getComponent(0).getComponentAt(0, 0);
		JLabel connectionStatus = (JLabel) frmTransfer.getContentPane().getComponent(0).getComponentAt(10, 0);
		
		try {
			
			// Attempting to connect.
			@SuppressWarnings("resource")
			Socket socket = new Socket(host, port);
			Socket rttSocket = new Socket(host, port);
			
			// Establish visual effects.
			connectionIndicator.setBackground(new Color(0, 150, 0));
			connectionStatus.setText(socket.getInetAddress().toString());
			
			// Initializing the data streams.
			dIN = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			dOUT = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
			(new Thread(new InputHandler(frmTransfer))).start();
			(new Thread(new RTTCalculator(rttSocket, (JLabel) frmTransfer.getContentPane().getComponent(0).getComponentAt(887, 21)))).start();
			
		} catch (UnknownHostException e) {
			connectionIndicator.setBackground(new Color(150, 0, 0));
			connectionStatus.setText("Host desconhecido.");
		} catch (IOException e) {
			connectionIndicator.setBackground(new Color(150, 0, 0));
			connectionStatus.setText("Conexão falhou.");
		}
		
	}
	
	private void handleConnection() {
		
		(new Thread(new Runnable() {
			
			private ServerSocket serverSocket;
			private Socket socket;
			private Socket rttSocket;
		
			@Override
			public void run() {
				
				JPanel connectionIndicator = (JPanel) frmTransfer.getContentPane().getComponent(0).getComponentAt(0, 0);
				JLabel connectionStatus = (JLabel) frmTransfer.getContentPane().getComponent(0).getComponentAt(10, 0);
				
				try {
					
					serverSocket = new ServerSocket(6000);
					
					// Wait for a connection.
					socket = serverSocket.accept();
					rttSocket = serverSocket.accept();
					
					// Establish visual effects.
					connectionIndicator.setBackground(new Color(0, 150, 0));
					connectionStatus.setText(socket.getInetAddress().toString());
					
					// Initializing the data streams.
					dIN = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					dOUT = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
					
					(new Thread(new InputHandler(frmTransfer, serverSocket))).start();
					(new Thread(new RTTCalculator(rttSocket, (JLabel) frmTransfer.getContentPane().getComponent(0).getComponentAt(887, 21)))).start();
					
					
				} catch (IOException e) {
					connectionIndicator.setBackground(new Color(150, 0, 0));
					connectionStatus.setText("Erro de conexão.");
				}
				
			}
			
		})).start();
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
					Transfer window = new Transfer();
					window.frmTransfer.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
