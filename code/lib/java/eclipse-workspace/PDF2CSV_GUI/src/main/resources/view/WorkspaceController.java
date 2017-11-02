package main.resources.view;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import image_processing.PageDrawer;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import textProcessing.Document;
import textProcessing.Word;


public class WorkspaceController implements Initializable {

	@FXML private ImageView imageView;
	@FXML private PdfOpTabController pdfOpTabController;
	private MainApp mainApp;
	private Document document;
	private PageDrawer pageDrawer;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		//imageView.setFitWidth(720);
		//imageView.setFitHeight(720);
		imageView.setPreserveRatio(true);
		pdfOpTabController.setRoot(this);
	}
	
	public void setRoot(MainApp mainApp) {
		this.mainApp = mainApp;
	}
	
	public void setDocument(Document document) {
		this.document = document;
		pageDrawer = new PageDrawer(document);
		BufferedImage image = document.getDocumentImage();
		setImage(image);
	}

	public void setImage(BufferedImage image) {
		Image imageFX = SwingFXUtils.toFXImage(image, null);
		imageView.setImage(imageFX);
	}
	
	public void findWords() {
		document.createWords();
		pageDrawer.drawWords();
		setImage(document.getDocumentImage());
	}
	
	public void createLines() {
		document.createLines();
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
}
