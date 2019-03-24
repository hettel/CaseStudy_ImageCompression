package app.util.ui;

import java.io.File;
import java.io.InputStream;

import javafx.scene.image.Image;

public class PreviewImage extends Image
{
  private static final int previewWidth = 500;
  private static final int previewHeight = 500;
  
  private File file;
  
  public PreviewImage(InputStream is, File file)
  {
    super(is, previewWidth, previewHeight, true, true);
    this.file = file;
  }

  public File getFile()
  {
    return this.file;
  }
}
