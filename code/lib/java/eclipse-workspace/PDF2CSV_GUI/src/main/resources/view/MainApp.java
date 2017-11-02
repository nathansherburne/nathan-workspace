package main.resources.view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import textProcessing.Document;

public class MainApp extends Application {
	
	Stage stage;
	FileChooser fileChooser = new FileChooser();
	@FXML private WorkspaceController workspaceController;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		init();
		Parent root = FXMLLoader.load(getClass().getResource("/main/resources/view/MainWindow.fxml"));
		Scene scene = new Scene(root, 1280, 1280);
		primaryStage.setTitle("PDF2CSV");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public void init() {
	}
	
	public void onExitClick() {
		System.exit(0);
	}
	
	public void onOpenClick() {
		fileChooser.setTitle("Open PDF File");
		File pdfFile = fileChooser.showOpenDialog(stage);
		if(pdfFile != null) {
			openPDF(pdfFile);
		}
	}

	private void openPDF(File pdfFile) {
		int pageNumber = 0;
		Document document = new Document(pdfFile, pageNumber);
		workspaceController.setDocument(document);
	}
	
	public void displayImage(BufferedImage img) {
		JFrame frame = new JFrame();
		  ImageIcon icon = new ImageIcon(img);
		  JLabel label = new JLabel(icon);
		  frame.add(label);
		  frame.setDefaultCloseOperation
		         (JFrame.EXIT_ON_CLOSE);
		  frame.pack();
		  frame.setVisible(true);
	}
	
	public void setWidth(double width) {
		stage.setWidth(width);
	}
	
	public void setHeight(double height) {
		stage.setHeight(height);
	}
}
