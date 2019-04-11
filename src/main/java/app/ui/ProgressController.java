package app.ui;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;

/**
 * Wrapper for a progress indicator. It is shown during the
 * calculation of the FFT 
 */
public class ProgressController implements Initializable
{
  @FXML
  private BorderPane mainPanel;
  
  @FXML
  private ProgressIndicator progressIndicator;
  

  @Override
  public void initialize(URL location, ResourceBundle resources)
  {
    
  }
}
