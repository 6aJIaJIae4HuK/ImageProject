package sample;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Controller {
    @FXML
    private TextField imagePath;

    @FXML
    private TextField segmentNumberInput;

    @FXML
    private ImageView inputImageView;

    @FXML
    private ImageView outputImageView;

    @FXML
    private void openDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open image");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Image files (*.bmp *.png *jpg)", "*.bmp", "*.png", "*.jpg");
        fileChooser.getExtensionFilters().add(filter);
        File file = fileChooser.showOpenDialog(rootStage);
        if (file != null)
            imagePath.setText(file.getAbsolutePath());
    }

    @FXML
    private void selectImage() {
        try {
            inputImage = ImageIO.read(new File(imagePath.getText()));
            Image image = SwingFXUtils.toFXImage(inputImage, null);
            inputImageView.setImage(image);
        } catch (IOException e) {
            showMessage(e.getMessage());
        }
    }

    @FXML
    private void segmentImage() {
        if (!check())
            return;

        ImageSegmentator segmentator = new KMeansSegmentator();

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                BufferedImage outputImage = segmentator.segment(inputImage, segmentNumber);
                Image image = SwingFXUtils.toFXImage(outputImage, null);
                outputImageView.setImage(image);
            }
        });
    }

    public void setStage(Stage stage) {
        rootStage = stage;
    }

    private boolean check() {
        if (inputImage == null) {
            showMessage("Select image to segment!");
            return false;
        }

        try {
            segmentNumber = Integer.parseInt(segmentNumberInput.getText());
            if (segmentNumber <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showMessage("Number must be correct positive number ");
            return false;
        }
        return true;
    }

    private void showMessage(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private Stage rootStage;
    private BufferedImage inputImage;
    private int segmentNumber;
}
