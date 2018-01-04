package sample;

import javafx.application.Platform;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.*;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class TreeSegmentator implements ImageSegmentator {
    public BufferedImage segment(BufferedImage inputImage, int segmentNumber) {
        this.width = inputImage.getWidth();
        this.height = inputImage.getHeight();
        this.inputImage = getColorArray(inputImage);
        this.outputImage = new int[this.inputImage.length];
        minSize = Math.max(width * height / (5 * segmentNumber), 1);
        segment(this.inputImage, segmentNumber);
        BufferedImage result = getImage(this.outputImage, inputImage.getAlphaRaster() != null);
        return result;
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

    private void segment(int[] image, int segmentNumber) {
        int[] drow = new int[] { 0, 1, 1, 1 };
        int[] dcol = new int[] { 1, -1, 0, 1 };
        graph = new ArrayList<>(width * height * 8);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                for (int d = 0; d < 4; d++) {
                    int newRow = row + drow[d];
                    int newCol = col + dcol[d];
                    if (0 <= newRow && newRow < height && 0 <= newCol && newCol < width) {
                        int p1 = row * width + col;
                        int p2 = newRow * width + newCol;
                        int weight = getWeight(inputImage[p1], inputImage[p2]);
                        graph.add(new Edge(p1, p2, weight));
                        graph.add(new Edge(p2, p1, weight));
                    }
                }
            }
        }

        graph.sort(Comparator.comparing(edge -> edge.weight));
        parent = new int[image.length];
        size = new int[image.length];
        for (int i = 0; i < image.length; i++) {
            parent[i] = i;
            size[i] = 1;
        }

        int setNumber = image.length;
        for (Edge edge : graph) {
            if (getParent(edge.from) == getParent(edge.to))
                continue;
            if (size[getParent(edge.from)] < minSize || size[getParent(edge.to)] < minSize) {
                unionSets(edge.from, edge.to);
                setNumber--;
            }
        }

        Comparator<Integer> setCompare = new Comparator<Integer>() {
            @Override
            public int compare(Integer firstSet, Integer secondSet) {
                if (size[firstSet] != size[secondSet])
                    return Integer.compare(size[firstSet], size[secondSet]);
                return Integer.compare(firstSet, secondSet);
            }
        };

        tree = new TreeSet[image.length];
        TreeSet<Integer> sets = new TreeSet<>(setCompare);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int ind1 = row * width + col;
                int p1 = getParent(ind1);
                if (tree[p1] == null) {
                    tree[p1] = new TreeSet<>(setCompare);
                    sets.add(p1);
                }
                for (int d = 0; d < 4; d++) {
                    int newRow = row + drow[d];
                    int newCol = col + dcol[d];
                    if (0 <= newRow && newRow < height && 0 <= newCol && newCol < width) {
                        int ind2 = newRow * width + newCol;
                        int p2 = getParent(ind2);
                        if (p1 != p2) {
                            if (tree[p2] == null) {
                                tree[p2] = new TreeSet<>(setCompare);
                                sets.add(p2);
                            }
                            tree[p1].add(p2);
                            tree[p2].add(p1);
                        }
                    }
                }
            }
        }

        while (setNumber > segmentNumber) {
            Integer set = sets.pollFirst();
            Integer adjSet = tree[set].first();
            List<Integer> setToAdd = new ArrayList<>();
            for (Integer s : sets) {
                tree[s].remove(set);
                if (tree[s].remove(adjSet))
                    setToAdd.add(s);
            }
            sets.remove(adjSet);
            if (unionSets(set, adjSet))
                setNumber--;
            sets.add(adjSet);
            for (Integer s : setToAdd) {
                tree[s].add(adjSet);
            }
            for (Integer s : tree[set]) {
                if (s != adjSet)
                    tree[adjSet].add(s);
            }
        }

        for (int ind = 0; ind < image.length; ind++)
            outputImage[ind] = inputImage[getParent(ind)];
    }

    private int getParent(int ind) {
        if (parent[ind] == ind)
            return ind;
        return parent[ind] = getParent(parent[ind]);
    }

    private boolean unionSets(int a, int b) {
        a = getParent(a);
        b = getParent(b);
        if (a == b)
            return false;
        if (size[a] < size[b]) {
            parent[a] = b;
            size[b] += size[a];
        } else {
            parent[b] = a;
            size[a] += size[b];
        }
        return true;
    }

    private int getWeight(int firstColor, int secondColor) {
        double firstRed = ((firstColor >> 16) & 0xFF) / 255.0;
        double firstGreen = ((firstColor >> 8) & 0xFF) / 255.0;
        double firstBlue = ((firstColor) & 0xFF) / 255.0;

        double secondRed = ((secondColor >> 16) & 0xFF) / 255.0;
        double secondGreen = ((secondColor >> 8) & 0xFF) / 255.0;
        double secondBlue = ((secondColor) & 0xFF) / 255.0;

        double firstGray = 0.2126 * firstRed + 0.7152 * firstGreen + 0.0722 * firstBlue;
        double secondGray = 0.2126 * secondRed + 0.7152 * secondGreen + 0.0722 * secondBlue;

        firstGray = Math.min(firstGray, 1.0);
        secondGray = Math.min(secondGray, 1.0);

        return (int)(Math.abs((firstGray - secondGray) * 1000));
    }

    private BufferedImage getImage(int[] colorArray, boolean alpha) {
        BufferedImage result = new BufferedImage(width, height, alpha ? TYPE_INT_ARGB : TYPE_INT_RGB);
        result.setRGB(0, 0, width, height, colorArray, 0, height);
        return result;
    }

    private class Edge {
        public int from;
        public int to;
        public int weight;
        public Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        @Override
        public int hashCode() {
            return from ^ to;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null)
                return false;
            if (other.getClass() != this.getClass())
                return false;
            Edge otherEdge = (Edge)other;
            return this.from == otherEdge.from && this.to == otherEdge.to ||
                    this.from == otherEdge.to && this.to == otherEdge.from;
        }
    }

    private int[] inputImage;
    private int[] outputImage;
    private int[] parent;
    private int[] size;
    private int width;
    private int height;
    private int minSize;
    private ArrayList<Edge> graph;
    private TreeSet<Integer>[] tree;
}
