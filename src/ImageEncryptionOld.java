import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.TextArea;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;

import sun.awt.shell.ShellFolder;

/**
 * Program which embeds data into an image creating a new version with
 * unnoticeable changes. When compared with the original, the data can be
 * retrieved.
 *
 * @author Ryan Jackman
 */
public class ImageEncryptionOld {

	private JFrame frame;
	private final JFileChooser fc = new JFileChooser();

	private JLabel keyImageLabel, codeImageLabel;
	private JLabel fileLabel;
	private TextArea textArea;

	private JButton encryptTextButton;
	private JButton decryptButton;
	private JButton encryptFileButton;
	private JMenuItem makeUniqueKeyButton;

	private BufferedImage key, code, output;
	private File file;

	private JCheckBoxMenuItem useAlphaLayerCheckBox;

	/**
	 * Creates a new instance of this class
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		new ImageEncryptionOld();
	}

	/**
	 * Sets the look and feel of the window and initializes the components
	 *
	 */
	public ImageEncryptionOld() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					initialize();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Creates all the components and listeners and places them in the window
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 658, 498);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);

		keyImageLabel = new JLabel("Image Key");
		keyImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		keyImageLabel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		codeImageLabel = new JLabel("Encoded Image");
		codeImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		codeImageLabel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		fileLabel = new JLabel("File Icon");
		fileLabel.setHorizontalAlignment(SwingConstants.CENTER);
		fileLabel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));

		encryptTextButton = new JButton("Encrypt Text");
		encryptTextButton.setEnabled(false);
		decryptButton = new JButton("Decrypt Image");
		decryptButton.setEnabled(false);
		encryptFileButton = new JButton("Encrypt File");
		encryptFileButton.setEnabled(false);

		JButton loadKeyImageButton = new JButton("Load Key Image");
		JButton loadEncodedImageButton = new JButton("Load Encoded Image");
		JButton btnLoadFile = new JButton("Load File");

		// Menu Bar
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		JMenuItem mntmClearAll = new JMenuItem("Clear All");

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		useAlphaLayerCheckBox = new JCheckBoxMenuItem("Use Alpha Layer");
		mnEdit.add(useAlphaLayerCheckBox);
		makeUniqueKeyButton = new JMenuItem("Create Unique Key");
		makeUniqueKeyButton.setEnabled(false);

		textArea = new TextArea();
		textArea.setFont(UIManager.getFont("TextArea.font"));

		JSeparator separator = new JSeparator();

		/*************
		 * CLEAR ALL *
		 *************/

		mntmClearAll.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				System.out.println("clear");
				code = null;
				key = null;
				file = null;

				keyImageLabel.setIcon(null);
				keyImageLabel.setText("Image Key");
				codeImageLabel.setIcon(null);
				codeImageLabel.setText("Encoded Image");

				textArea.setText("");

				fileLabel.setIcon(null);
				fileLabel.setText("File Icon");

				encryptFileButton.setEnabled(false);
				decryptButton.setEnabled(false);
				encryptTextButton.setEnabled(false);
				makeUniqueKeyButton.setEnabled(false);
			}
		});
		mnFile.add(mntmClearAll);

		/********
		 * EXIT *
		 ********/

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);

		/*******************
		 * MAKE UNIQUE KEY *
		 *******************/

		makeUniqueKeyButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (key != null) {
					BufferedImage temp = deepCopy(key);
					for (int y = 0; y < temp.getHeight(); y++) {
						for (int x = 0; x < temp.getWidth(); x++) {
							int rgb = temp.getRGB(x, y);
							int[] c = { (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb) & 0xFF };

							for (int i = 0; i < 3; i++) {
								if (Math.random() > 0.7) {
									c[i] += 1;
								}
							}
						}
					}

					File f = null;
					fc.setSelectedFile(new File("image.png"));
					int returnVal = fc.showSaveDialog(frame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						f = fc.getSelectedFile();

						try {
							ImageIO.write(temp, "png", f);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		});
		mnEdit.add(makeUniqueKeyButton);

		/***************
		 * LOAD IMAGES *
		 ***************/

		loadKeyImageButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				key = loadImage();
				if (key != null) {
					keyImageLabel.setIcon(getScaledIcon(key, 170, 177));
					keyImageLabel.setText("");

					makeUniqueKeyButton.setEnabled(true);
					encryptTextButton.setEnabled(true);
					if(code != null)
						decryptButton.setEnabled(true);
					if(file != null)
						encryptFileButton.setEnabled(true);
				}
			}
		});

		loadEncodedImageButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				code = loadImage();
				if (code != null) {
					codeImageLabel.setIcon(getScaledIcon(code, 170, 177));
					codeImageLabel.setText("");

					if(key != null)
						decryptButton.setEnabled(true);
				}
			}
		});

		/*************
		 * LOAD FILE *
		 *************/

		btnLoadFile.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				int returnVal = fc.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = fc.getSelectedFile();
					try {
						fileLabel.setIcon(getScaledIcon((BufferedImage) ShellFolder.getShellFolder(file).getIcon(true), 91, 104));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					fileLabel.setText("");

					if(key != null)
						encryptFileButton.setEnabled(true);
				}
			}
		});

		/*****************
		 * DECRYPT IMAGE *
		 *****************/

		decryptButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Byte array for "text"
				byte[] t = { 116, 101, 120, 116 };
				byte[] b = decrypt(0);

				if (Arrays.equals(b, t)) {
					// The image holds text
					Object[] options = { "Print", "Save", "Cancel" };
					int n = JOptionPane.showOptionDialog(frame, "This image contains text. What would you like to do?", "Message",
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

					System.out.println(n);

					// Convert bytes to a string and put it in the text area
					if (n == 0) {
						b = decrypt(1);
						StringBuilder output = new StringBuilder();
						;
						for (byte bt : b) {
							output.append((char) bt);
						}
						textArea.setText(output.toString());
					}
				} else {
					// The image holds a file
					Object[] options = { "Save", "Cancel" };
					int n = JOptionPane.showOptionDialog(frame, "This image contains a file. What would you like to do?", "Message",
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

					// Open save dialog and save file to chosen location
					if (n == 0) {
						File temp = null;

						fc.setSelectedFile(new File(new String(b)));
						int returnVal = fc.showSaveDialog(frame);

						if (returnVal == JFileChooser.APPROVE_OPTION) {
							temp = fc.getSelectedFile();

							FileOutputStream fout;
							try {
								fout = new FileOutputStream(temp);
								fout.write(decrypt(1));
								fout.close();
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
		});

		/****************
		 * ENCRYPT TEXT *
		 ****************/

		encryptTextButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				output = deepCopy(key);
				encrypt("text".getBytes(), 0);
				encrypt(textToBytes(), 1);

				File f = null;
				fc.setSelectedFile(new File("result.png"));
				int returnVal = fc.showSaveDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					f = fc.getSelectedFile();

					try {
						ImageIO.write(output, "png", f);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				textArea.setText("");
			}
		});

		/****************
		 * ENCRYPT FILE *
		 ****************/

		encryptFileButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				if (file.length() > (key.getWidth() * key.getHeight()-1) * (3.0/8.0) - 100){
					JOptionPane.showMessageDialog(frame, "This file to too large. Cannot encrypt", "Warning", JOptionPane.WARNING_MESSAGE);
					return;
				}

				System.out.println("got pressed");
				output = deepCopy(key);
				encrypt(file.getName().getBytes(), 0);
				encrypt(fileToBytes(), 1);

				File f = null;
				fc.setSelectedFile(new File("result.png"));
				int returnVal = fc.showSaveDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					f = fc.getSelectedFile();

					try {
						ImageIO.write(output, "png", f);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					textArea.setText("");
				}
			}
		});

		/**********
		 * LAYOUT * Auto generated by Eclipse WindowBuilder
		 **********/

		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
								.addComponent(codeImageLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(loadKeyImageButton, GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
								.addComponent(keyImageLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(loadEncodedImageButton, GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE))
								.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
										.addGroup(groupLayout.createSequentialGroup()
												.addGap(12)
												.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
														.addComponent(textArea, GroupLayout.PREFERRED_SIZE, 434, Short.MAX_VALUE)
														.addGroup(groupLayout.createSequentialGroup()
																.addPreferredGap(ComponentPlacement.RELATED)
																.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
																		.addComponent(separator, GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)
																		.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
																				.addComponent(fileLabel, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
																				.addPreferredGap(ComponentPlacement.RELATED)
																				.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
																						.addGroup(groupLayout.createSequentialGroup()
																								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
																										.addComponent(btnLoadFile, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																										.addComponent(encryptFileButton, GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE))
																										.addGap(224))
																										.addComponent(decryptButton)))))))
																										.addGroup(groupLayout.createSequentialGroup()
																												.addPreferredGap(ComponentPlacement.RELATED)
																												.addComponent(encryptTextButton)))
																												.addContainerGap())
				);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addGap(10)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
										.addComponent(textArea, GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
										.addGap(10)
										.addComponent(encryptTextButton)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(separator, GroupLayout.PREFERRED_SIZE, 2, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
														.addGap(3)
														.addComponent(fileLabel, GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
														.addGroup(groupLayout.createSequentialGroup()
																.addComponent(btnLoadFile)
																.addPreferredGap(ComponentPlacement.RELATED)
																.addComponent(encryptFileButton)
																.addPreferredGap(ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
																.addComponent(decryptButton))))
																.addGroup(groupLayout.createSequentialGroup()
																		.addGap(1)
																		.addComponent(loadKeyImageButton)
																		.addPreferredGap(ComponentPlacement.RELATED)
																		.addComponent(loadEncodedImageButton)
																		.addPreferredGap(ComponentPlacement.RELATED)
																		.addComponent(keyImageLabel, GroupLayout.PREFERRED_SIZE, 177, GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(ComponentPlacement.RELATED)
																		.addComponent(codeImageLabel, GroupLayout.PREFERRED_SIZE, 177, GroupLayout.PREFERRED_SIZE)))
																		.addGap(10))
				);
		frame.getContentPane().setLayout(groupLayout);
		frame.setVisible(true);
	}

	/**
	 * @return byte array for the text or file being encrypted
	 */
	private byte[] textToBytes() {
		return textArea.getText().getBytes();
	}

	/**
	 * @return byte array of the selected file
	 */
	private byte[] fileToBytes() {
		try {
			Path path = Paths.get(file.getAbsolutePath());
			return Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Encodes a byte array into the image starting at pixel row ys
	 *
	 * @param bytes array of bytes to be written to the image
	 * @param ys pixel row to start encoding on
	 */
	private void encrypt(byte[] bytes, int ys) {

		// Builds a literal binary string from the byte array
		StringBuilder binary = new StringBuilder();
		for (byte b : bytes) {
			int val = b;
			for (int i = 0; i < 8; i++) {
				binary.append((val & 128) == 0 ? 0 : 1);
				val <<= 1;
			}
		}

		// TODO Make this use a boolean array to save memory
		char[] ca = binary.toString().toCharArray();
		int n = 0;
		int rgb;
		outerloop: for (int y = ys; y < output.getHeight(); y++) {
			for (int x = 0; x < output.getWidth(); x++) {

				// For each pixel, get the RGB subpixel values
				rgb = output.getRGB(x, y);
				// TODO Implement optional alpha channel
				int alpha = (rgb >> 24) & 0xFF;
				int[] c = { (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb) & 0xFF };

				for (int i = 0; i < 3; i++) {

					// Offset each value by one if bit equals one
					if (n < binary.length()) {
						if (ca[n] == '1') {
							if (c[i] != 255)
								c[i]++;
							else
								c[i]--;
						}
						n++;
					} else {
						// Offset subpixel by two to signify end of bit stream
						if (c[i] < 254)
							c[i] += 2;
						else
							c[i] -= 2;
						System.out.println("adding break at " + i + " " + x + " " + y);
						Color color = new Color(c[0], c[1], c[2], alpha);
						output.setRGB(x, y, color.getRGB());

						break outerloop;
					}
				}
				Color color = new Color(c[0], c[1], c[2], alpha);
				output.setRGB(x, y, color.getRGB());
			}
		}
		
	}

	/**
	 * Returns a byte array from the image starting at row ys
	 *
	 * @param ys decrypt starting on specified row
	 * @return byte array from the image
	 */
	private byte[] decrypt(int ys) {
		StringBuilder binary = new StringBuilder();
		int rgbc, rgbk;
		outerloop: for (int y = ys; y < key.getHeight(); y++) {
			for (int x = 0; x < key.getWidth(); x++) {
				// For each subpixel in the current pixel, append the difference between key and code image
				rgbk = key.getRGB(x, y);
				rgbc = code.getRGB(x, y);

				int rb = getBinary((rgbk >> 16) & 0xFF, (rgbc >> 16) & 0xFF);
				int gb = getBinary((rgbk >> 8) & 0xFF, (rgbc >> 8) & 0xFF);
				int bb = getBinary((rgbk) & 0xFF, (rgbc) & 0xFF);

				int[] b = { rb, gb, bb };

				for (int i = 0; i < 3; i++) {
					if (b[i] == 2) {
						break outerloop;
					} else
						binary.append(Integer.toString(b[i]));
				}
			}
		}

		// Change string of bits into byte array and return
		byte[] b = new byte[binary.toString().length() / 8];
		int i = 0;
		for (String st : getParts(binary.toString(), 8)) {
			b[i] = (byte) Integer.parseInt(st, 2);
			i++;
		}

		return b;
	}

	/**
	 * Returns an integer bit or 2 to signify the end of the bit stream
	 *
	 * @param k key image pixel value
	 * @param c code image pixel value
	 * @return the absolute difference in the values
	 */
	private int getBinary(int k, int c) {
		if (Math.abs(k - c) == 1)
			return 1;
		if (Math.abs(k - c) == 0)
			return 0;
		return 2;
	}

	/**
	 * Opens a file loading box and prompts the user for an image
	 *
	 * @return a loaded buffered image
	 */
	private BufferedImage loadImage() {
		int returnVal = fc.showOpenDialog(frame);
		File file;
		BufferedImage image = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fc.getSelectedFile();
			try {
				image = ImageIO.read(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return image;
	}

	/**
	 * Returns an ImageIcon scaled to fit within w by h without altering the aspect ratio
	 *
	 * @param image Image to be scaled
	 * @param w max width
	 * @param h max height
	 * @return scaled icon from the image
	 */
	private ImageIcon getScaledIcon(BufferedImage image, int w, int h) {

		int mWidth = w, mHeight = h;
		double scale;
		if (mWidth / image.getWidth() > mHeight / image.getHeight())
			scale = mHeight / (double) image.getHeight();
		else
			scale = mWidth / (double) image.getWidth();

		return new ImageIcon(image.getScaledInstance((int) (image.getWidth() * scale), (int) (image.getHeight() * scale), Image.SCALE_FAST));
	}

	/**
	 * Splits the string into units partitionSize long
	 *
	 * @param string String to be split
	 * @param partitionSize size of substrings
	 * @return list of substrings
	 */
	private static List<String> getParts(String string, int partitionSize) {
		List<String> parts = new ArrayList<String>();
		int len = string.length();
		for (int i = 0; i < len; i += partitionSize) {
			parts.add(string.substring(i, Math.min(len, i + partitionSize)));
		}
		return parts;
	}

	/**
	 * Returns a separate or 'deep' copy of a BufferedImage
	 *
	 * @param bi image to copy
	 * @return copy of image
	 */
	static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
}