package sample;

import java.awt.image.BufferedImage;

public interface ImageSegmentator {
    BufferedImage segment(BufferedImage inputImage, int segmentNumber);
}
