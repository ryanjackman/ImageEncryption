/*******************************************************************************
 * Copyright 2013-2014 Andreas Holley, Ryan Jackman
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.github.imageencryption;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class containing methods for ImageEncryption.
 * 
 * @author Andreas Holley
 * 
 */
public class ImageEncryption {

	/**
	 * Get an image containing encrypted text.
	 * 
	 * @param key
	 *            the {@link BufferedImage} to use as the key.
	 * @param text
	 *            the text to encrypt.
	 * @return the encrypted image.
	 */
	public static BufferedImage encryptText(BufferedImage key, String text) {
		return encrypt(encrypt(key, "text".getBytes(), 0), text.getBytes(), 1);
	}

	/**
	 * Get an image containing an encrypted file.
	 * 
	 * @param key
	 *            the {@link BufferedImage} to use as the key.
	 * @param file
	 *            the file to encrypt.
	 * @return the encrypted image.
	 */
	public static BufferedImage encryptFile(BufferedImage key, File file) {
		return encrypt(encrypt(key, file.getName().getBytes(), 0),
				fileToBytes(file), 1);
	}

	/**
	 * Decrypt an image that has been encrypted to store text.
	 * 
	 * @param key
	 *            the key image that was used to encrypt the image.
	 * @param code
	 *            the encrypted image that contains the text.
	 * @return the encrypted text.
	 */
	public static String decryptText(BufferedImage key, BufferedImage code) {
		return new String(decrypt(key, code, 1));
	}

	/**
	 * Decrypt an image that has been encrypted to store a file.
	 * 
	 * @param key
	 *            the key image that was used to encrypt the image.
	 * @param code
	 *            the encrypted image that contains the text.
	 * @return a byte[] of the file's contents.
	 */
	public static byte[] decryptFile(BufferedImage key, BufferedImage code) {
		return decrypt(key, code, 1);
	}

	private static byte[] decrypt(BufferedImage key, BufferedImage code, int ys) {
		StringBuilder binary = new StringBuilder();
		int rgbc, rgbk;
		outerloop: for (int y = ys; y < key.getHeight(); y++) {
			for (int x = 0; x < key.getWidth(); x++) {
				// For each subpixel in the current pixel, append the difference
				// between key and code image
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

	private static List<String> getParts(String string, int partitionSize) {
		List<String> parts = new ArrayList<String>();
		int len = string.length();
		for (int i = 0; i < len; i += partitionSize) {
			parts.add(string.substring(i, Math.min(len, i + partitionSize)));
		}
		return parts;
	}

	private static int getBinary(int k, int c) {
		if (Math.abs(k - c) == 1)
			return 1;
		if (Math.abs(k - c) == 0)
			return 0;
		return 2;
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
						System.out.println("adding break at " + i + " " + x
								+ " " + y);
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

	private static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	private static byte[] fileToBytes(File file) {
		try {
			Path path = Paths.get(file.getAbsolutePath());
			return Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		BufferedImage key = ImageIO.read(new File(
				"C:\\Users\\Andreas\\Desktop\\image.png"));
		BufferedImage result = encryptText(key, "Hello, world!");
		ImageIO.write(result, "png", new File(
				"C:\\Users\\Andreas\\Desktop\\result.png"));
		System.out.println(decryptText(key, result).equals("Hello, world!"));
		System.out.println("Finished!");
	}
}
