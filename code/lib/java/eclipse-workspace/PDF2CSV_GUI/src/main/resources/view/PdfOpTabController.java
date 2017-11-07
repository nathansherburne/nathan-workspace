package main.resources.view;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

public class PdfOpTabController implements Initializable {

	@FXML private ListView<String> pdfOpList;
	private WorkspaceController workspaceController;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		pdfOpList.getItems().addAll(getOperations());
	}
	
	public void setRoot(WorkspaceController workspaceController) {
		this.workspaceController = workspaceController;
	}
	
	public void onGoButtonClick() {
		String selectedOperation = pdfOpList.getSelectionModel().getSelectedItem();
		if(selectedOperation.equals("Draw Word Bounds")) {
			workspaceController.drawWords();
		} else if(selectedOperation.equals("Draw Block Bounds")) {
			workspaceController.drawBlocks();
		} else if(selectedOperation.equals("Draw Line Bounds")) {
			workspaceController.drawLines();
		} else if(selectedOperation.equals("Isolate Merged Columns")) {
			workspaceController.isolateMergedColumns();
		} 
	}
	
	private List<String> getOperations() {
		List<String> ops = new ArrayList<>();
		ops.add("Draw Word Bounds");
		ops.add("Draw Block Bounds");
		ops.add("Draw Line Bounds");
		ops.add("Isolate Merged Columns");
		ops.add("Create Neighborhoods");
		ops.add("Merge Isolated Blocks");
		ops.add("Decompose Type 1 Blocks");
		return ops;
	}

}
