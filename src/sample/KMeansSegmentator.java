package sample;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class KMeansSegmentator implements ImageSegmentator {
    public BufferedImage segment(BufferedImage inputImage, int segmentNumber)
    {
        this.width = inputImage.getWidth();
        this.height = inputImage.getHeight();
        this.inputImage = getColorArray(inputImage);
        this.outputImage = new int[width * height];
        this.claster = new int[width * height];
        initCenters(segmentNumber);
        do {
            fillBuffer();
            reCalculateCenters();
            swapBuffers();
        } while (isChanged());
        return getImage(outputImage, inputImage.getAlphaRaster() != null);
    }

    private void swapBuffers() {
        int[] tmp = inputImage;
        inputImage = outputImage;
        outputImage = tmp;
    }

    private int[] getColorArray(BufferedImage image) {
        boolean alpha = image.getAlphaRaster() != null;
        byte[] data = ((DataBufferByte)image.getData().getDataBuffer()).getData();
        int[] result = new int[width * height];
        int ind = 0;
        for (int pixel = 0; pixel < width * height; pixel++) {
            int argb = 0;
            if (alpha)
                argb += (((int)data[ind++] & 0xFF) << 24);
            else
                argb += 0xFF000000;
            argb += (((int)data[ind++] & 0xFF) << 0);
            argb += (((int)data[ind++] & 0xFF) << 8);
            argb += (((int)data[ind++] & 0xFF) << 16);
            result[pixel] = argb;
        }
        return result;
    }

    private BufferedImage getImage(int[] colorArray, boolean alpha) {
        BufferedImage result = new BufferedImage(width, height, alpha ? TYPE_INT_ARGB : TYPE_INT_RGB);
        result.setRGB(0, 0, width, height, colorArray, 0, width);
        return result;
    }

    private void initCenters(int num) {
        List<Integer> nums = Arrays.stream(IntStream.range(0, width * height).toArray()).boxed().collect(Collectors.toList());
        Collections.shuffle(nums);
        int cur = 0;
        curCenters = new int[num];
        for (Integer n : nums) {
            curCenters[cur++] = n;
            if (cur >= num)
                break;
        }
    }

    private boolean isChanged() {
        for (int i = 0; i < width * height; i++) {
            if (inputImage[i] != outputImage[i])
                return true;
        }
        return false;
    }

    private int getWeight(int firstColor, int secondColor) {
        int firstRed = (firstColor >> 16) & 0xFF;
        int firstGreen = (firstColor >> 8) & 0xFF;
        int firstBlue = firstColor & 0xFF;

        int secondRed = (secondColor >> 16) & 0xFF;
        int secondGreen = (secondColor >> 8) & 0xFF;
        int secondBlue = secondColor & 0xFF;

        return (firstRed - secondRed) * (firstRed - secondRed) +
                (firstGreen - secondGreen) * (firstGreen - firstGreen) +
                (firstBlue - secondBlue) * (firstBlue - secondBlue);
    }

    private void fillBuffer() {
        for (int i = 0; i < width * height; i++) {
            int color = -1;
            int ind = -1;
            int dist = Integer.MAX_VALUE;
            for (int j = 0; j < curCenters.length; j++) {
                int center = curCenters[j];
                int weight = getWeight(inputImage[i], inputImage[center]);
                if (weight < dist) {
                    dist = weight;
                    color = inputImage[center];
                    ind = j;
                }
            }
            outputImage[i] = color;
            claster[i] = ind;
        }
        for (int i = 0; i < curCenters.length; i++) {
            claster[curCenters[i]] = i;
        }
    }

    private void reCalculateCenters() {
        int[] rowSum = new int[curCenters.length];
        int[] colSum = new int[curCenters.length];
        int[] cnt = new int[curCenters.length];

        for (int i = 0; i < width * height; i++) {
            int ind = claster[i];
            rowSum[ind] += (i / width);
            colSum[ind] += (i % width);
            cnt[ind]++;
        }

        int nonZeroCount = 0;
        for (int i = 0; i < curCenters.length; i++) {
            if (cnt[i] > 0)
                nonZeroCount++;
        }

        int[] newCenters = new int[nonZeroCount];
        int curInd = 0;
        for (int i = 0; i < curCenters.length; i++) {
            if (cnt[i] > 0) {
                rowSum[i] /= cnt[i];
                colSum[i] /= cnt[i];
                newCenters[curInd] = rowSum[i] * width + colSum[i];
                curInd++;
            }
        }
        curCenters = newCenters;
    }

    private int width;
    private int height;
    private int[] curCenters;
    private int[] inputImage;
    private int[] outputImage;
    private int[] claster;
}
