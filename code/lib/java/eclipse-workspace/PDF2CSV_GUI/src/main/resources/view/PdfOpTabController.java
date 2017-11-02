package main.resources.view;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

public class PdfOpTabController implements Initializable {

	@FXML private ListView<String> pdfOpList;
	private WorkspaceController workspaceController;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		pdfOpList.getItems().add("Find Words");
	}
	
	public void setRoot(WorkspaceController workspaceController) {
		this.workspaceController = workspaceController;
	}
	
	public void onGoButtonClick() {
		String selectedOperation = pdfOpList.getSelectionModel().getSelectedItem();
		if(selectedOperation.equals("Find Words")) {
			workspaceController.findWords();
		}
	}

}
