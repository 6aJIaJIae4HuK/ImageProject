package sample;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class TreeSegmentator implements ImageSegmentator {
    public BufferedImage segment(BufferedImage inputImage, int segmentCount) {
        this.inputImage = getColorMatrix(inputImage);
        minPixelNumber = inputImage.getHeight() * inputImage.getWidth() / (segmentCount * 3);
        outputImage = segment(this.inputImage);
        BufferedImage result = getImage(this.outputImage, inputImage.getAlphaRaster() != null);
        return result;
    }

    private int[][] getColorMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean alpha = image.getAlphaRaster() != null;
        byte[] data = ((DataBufferByte)image.getData().getDataBuffer()).getData();
        int[][] result = new int[height][width];
        int ind = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int argb = 0;
                if (alpha)
                    argb += (((int)data[ind++] & 0xFF) << 24);
                else
                    argb += 0xFF000000;
                argb += (((int)data[ind++] & 0xFF) << 0);
                argb += (((int)data[ind++] & 0xFF) << 8);
                argb += (((int)data[ind++] & 0xFF) << 16);
                result[row][col] = argb;
            }
        }
        return result;
    }

    private int[][] segment(int[][] image) {
        return image;
    }

    private BufferedImage getImage(int[][] colorMatrix, boolean alpha) {
        int width = colorMatrix[0].length;
        int height = colorMatrix.length;
        BufferedImage result = new BufferedImage(width, height, alpha ? TYPE_INT_ARGB : TYPE_INT_RGB);
        int[] argbArray = new int[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(colorMatrix[row], 0, argbArray, row * width, width);
        }
        result.setRGB(0, 0, width, height, argbArray, 0, height);
        return result;
    }

    private int[][] inputImage;
    private int[][] outputImage;
    private int minPixelNumber;

}
