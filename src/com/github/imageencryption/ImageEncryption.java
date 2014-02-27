package com.github.imageencryption;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Class containing methods for ImageEncryption.
 * @author Andreas Holley
 *
 */
public class ImageEncryption {
	
	/**
	 * Get an image containing encrypted text.
	 * @param key the {@link BufferedImage} to use as the key.
	 * @param text the text to encrypt.
	 * @return the encrypted image.
	 */
	public static BufferedImage encryptText(BufferedImage key, String text) {
		return encrypt(encrypt(key, "text".getBytes(), 0), text.getBytes(), 1);
	}

	private static BufferedImage encrypt(BufferedImage key, byte[] bytes, int ys) {
		BufferedImage encrypted = deepCopy(key);
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
		outerloop: for (int y = ys; y < encrypted.getHeight(); y++) {
			for (int x = 0; x < encrypted.getWidth(); x++) {

				// For each pixel, get the RGB subpixel values
				rgb = encrypted.getRGB(x, y);
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
						encrypted.setRGB(x, y, color.getRGB());

						break outerloop;
					}
				}
				Color color = new Color(c[0], c[1], c[2], alpha);
				encrypted.setRGB(x, y, color.getRGB());
			}
		}

		return encrypted;
	}

	/**
	 * Clones a BufferedImage object by performing a deep copy.
	 * 
	 * @param bi
	 *            the image to clone.
	 * @return a deep copy of the image.
	 */
	private static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static void main(String[] args) throws IOException {
		BufferedImage key = ImageIO.read(new File("C:\\Users\\Andreas\\Desktop\\image.png"));
		BufferedImage result = encryptText(key, "Hello, world!");
		ImageIO.write(result, "png", new File("C:\\Users\\Andreas\\Desktop\\result.png"));
		System.out.println("Finished!");
	}
}
