package app.ui;

import static app.util.fft.FFT.fft2;
import static app.util.fft.FFT.ifft2;
import static app.util.fft.FFTUtils.extractSubMatrix;
import static app.util.fft.FFTUtils.getAbsValuesOfMatrix;
import static app.util.fft.FFTUtils.getLogScaledBufferedImageForMatix;
import static app.util.fft.FFTUtils.getPaddedPowerOf2MatrixForPixelBuffer;
import static app.util.fft.FFTUtils.getSortedElements;
import static app.util.fft.FFTUtils.getPixelBufferFor;
import static app.util.fft.FFTUtils.shiftAbsFourierKoef;
import static app.util.fft.FFTUtils.createNewGrayScaleBuffer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


import app.util.fft.FFTUtils;
import app.util.ui.FileIOHelper;
import app.util.ui.PreviewImage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import tool.hal.CpuInfoPublisher;

public class UIController implements Initializable
{
  @FXML
  private BorderPane mainWindow;

  @FXML
  private VBox fftImageContainer;

  @FXML
  private ImageView mainImageView;

  @FXML
  private ImageView fftKoefImageView;

  @FXML
  private ImageView fftImageView;

  @FXML
  private ImageView fftTruncKoefImageView;

  @FXML
  private HBox imageBox;

  @FXML
  private Button startBtn;

  @FXML
  private Slider qualitySlider;
  @FXML
  private Label qualityValue;

  @FXML
  private Rectangle cpuLoadBar;
  @FXML
  private Label cpuLabel;

  // Loading Flag
  private AtomicBoolean isCalculatingImage = new AtomicBoolean(false);
  
  // FFT-Variables
  private double[][] fftMatrix;
  private double[][] fftMatrixAbsValue;
  private CompletableFuture<List<Double>> sortedKoefCF;
  private double maxValueCF;

  @FXML
  public void open()
  {
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("Select Gallery");
    dirChooser.setInitialDirectory(Paths.get(".").toFile());
    File file = dirChooser.showDialog(mainWindow.getScene().getWindow());

    if (file != null && file.isDirectory())
    {
      // Delete old image content
      // mainImageView.setImage(null);
      imageBox.getChildren().clear();

      long time = System.currentTimeMillis();
      List<ImageView> imageViews = FileIOHelper.loadPreViewImages(file);
      System.out.println("Time to load preview images : " + (System.currentTimeMillis() - time) + "[ms]");

      for (ImageView imageView : imageViews)
      {
        imageView.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
          @Override
          public void handle(MouseEvent mouseEvent)
          {
            if( isCalculatingImage.get() ) return;
            
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY))
            {
              if (mouseEvent.getClickCount() == 1)
              {
                isCalculatingImage.set(true);
                fftImageView.setImage(null);
                fftKoefImageView.setImage(null);
                fftTruncKoefImageView.setImage(null);
                File file = ((PreviewImage) imageView.getImage()).getFile();
                loadImageConvertToGrayScaleAndCalculateFourierCoeffiecents(file);
              }
            }
          }
        });
      }

      this.imageBox.getChildren().addAll(imageViews);
    }
  }


  @FXML
  public void applyFFT()
  {
    if (this.mainImageView.getImage() == null)
      return;

    this.showProgressIndicator();
 
    // --- Truncate Koef
    double compressRate = this.qualitySlider.getValue();

    List<Double> sortedElement = this.sortedKoefCF.join();
    int thresholdPos = (int) (compressRate * sortedElement.size());
    double threshold = sortedElement.get(thresholdPos);

    CompletableFuture<double[][]> fftTruncatedMatrix = CompletableFuture.supplyAsync(() -> FFTUtils.nullifyKoefficients(this.fftMatrix, threshold));

    CompletableFuture<?> task1 = fftTruncatedMatrix.thenApplyAsync(fftTruncMatrix -> {
      double[][] fftTruncatedMatrixAbsValue = getAbsValuesOfMatrix(fftTruncMatrix);
      double[][] fftTruncatedMatrixAbsValueShifted = shiftAbsFourierKoef(fftTruncatedMatrixAbsValue);
      BufferedImage fftTruncatedImage = getLogScaledBufferedImageForMatix(fftTruncatedMatrixAbsValueShifted, this.maxValueCF);
      return fftTruncatedImage;
    }).thenAcceptAsync(fftTruncatedImage -> this.fftTruncKoefImageView.setImage(SwingFXUtils.toFXImage(fftTruncatedImage, null)), Platform::runLater)
        .orTimeout(15, TimeUnit.SECONDS).exceptionally(exce -> {
          exce.printStackTrace();
          return (Void) null;
        });

    // --- iFFT ---
    Image image = this.mainImageView.getImage();
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();
    
    AtomicBoolean isCanceled = new AtomicBoolean(false);
    CompletableFuture<?> task2 = fftTruncatedMatrix.thenApplyAsync(fftTruncMatrix -> {

      double[][] ifftReducedImageMatrix = ifft2(fftTruncMatrix);
      
      if( isCanceled.get() ) { System.err.println("Break after Step 1"); return null; }
      
      double[][] ifftReducedImageMatrixPadded = extractSubMatrix(ifftReducedImageMatrix, width, height);
      
      if( isCanceled.get() ) { System.err.println("Break after Step 2"); return null; }
      
      double[][] ifftReducedImageMatrixPaddedAbsValue = getAbsValuesOfMatrix(ifftReducedImageMatrixPadded);

      if( isCanceled.get() ) { System.err.println("Break after Step 3"); return null; }
      
      int[] pixelBuffer = getPixelBufferFor(ifftReducedImageMatrixPaddedAbsValue);
      BufferedImage imageOut = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); // Erzeuge neues Bild
      for (int i = 0; i < pixelBuffer.length; i++)
      {
        int argb = pixelBuffer[i];
        int x = i % width;
        int y = i / width;
        imageOut.setRGB(x, y, argb);
      }

      return imageOut;
    }).thenAcceptAsync(imageOut -> this.fftImageView.setImage(SwingFXUtils.toFXImage(imageOut, null)), Platform::runLater).orTimeout(10, TimeUnit.SECONDS)
        .exceptionally(exce -> {
          isCanceled.set(true);
          exce.printStackTrace();
          return (Void) null;
        });

    task1.thenCombineAsync(task2, (v1, v2) -> closeProgressIndicator(), Platform::runLater);
  }

  public byte[] convertIntegersToBytes(int[] integers)
  {
    if (integers != null)
    {
      byte[] outputBytes = new byte[integers.length * 4];

      for (int i = 0, k = 0; i < integers.length; i++)
      {
        int integerTemp = integers[i];
        for (int j = 0; j < 4; j++, k++)
        {
          outputBytes[k] = (byte) ((integerTemp >> (8 * j)) & 0xFF);
        }
      }
      return outputBytes;
    }
    else
    {
      return null;
    }
  }

  @FXML
  public void exit()
  {
    Platform.exit();
  }

  private CpuInfoPublisher publisher;
  private DecimalFormat df = new DecimalFormat("#0.0");

  @Override
  public void initialize(URL location, ResourceBundle resources)
  {
    this.startBtn.setDisable(true);
    this.cpuLoadBar.setFill(Color.GREEN);

    this.qualityValue.textProperty().bind(qualitySlider.valueProperty().asString("%6.2f"));

    CompletableFuture.runAsync(() -> {
      // Initialize the hardware detection for getting the CPU load
      publisher = CpuInfoPublisher.getInstance();
      publisher.subscribe(value -> Platform.runLater(() -> {
        repaintGradient(value);
        cpuLabel.setText(df.format(100 * value) + " %");
      }));
    }).orTimeout(3, TimeUnit.SECONDS).exceptionally(ex -> {
      ex.printStackTrace();
      return (Void) null;
    });
  }

  // Color gradient for CPU load
  private final Stop[] stops = new Stop[] { new Stop(0, Color.GREEN), new Stop(1, Color.RED) };

  private void repaintGradient(double value)
  {
    LinearGradient linGradient = new LinearGradient(0, 0, 0, 2 * (1 - value) * cpuLoadBar.getHeight(), false, CycleMethod.NO_CYCLE, stops);
    cpuLoadBar.setFill(linGradient);
  }

  private void loadImageConvertToGrayScaleAndCalculateFourierCoeffiecents(File file)
  {
    try
    {
      this.startBtn.setDisable(true);

      byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
      InputStream iStream = new ByteArrayInputStream(bytes);
      Image image = new Image(iStream);

      PixelReader pReader = image.getPixelReader();
      int width = (int) image.getWidth();
      int height = (int) image.getHeight();

      WritablePixelFormat<IntBuffer> format = WritablePixelFormat.getIntArgbInstance();

      int[] pixelBuffer = new int[width * height];
      pReader.getPixels(0, 0, width, height, format, pixelBuffer, 0, width);
      int[] grayPixelBuffer = createNewGrayScaleBuffer(pixelBuffer);

      CompletableFuture.supplyAsync(() -> {
        double[][] imageMatrix = getPaddedPowerOf2MatrixForPixelBuffer(grayPixelBuffer, width, height);
        this.fftMatrix = fft2(imageMatrix);
        this.fftMatrixAbsValue = getAbsValuesOfMatrix(fftMatrix);

        this.sortedKoefCF = CompletableFuture.supplyAsync(() -> getSortedElements(fftMatrixAbsValue));

        double[][] fftMatrixAbsValueShifted = FFTUtils.shiftAbsFourierKoef(fftMatrixAbsValue);

        this.maxValueCF = this.sortedKoefCF.join().get(this.sortedKoefCF.join().size() - 1);
        BufferedImage fftBufferedImage = getLogScaledBufferedImageForMatix(fftMatrixAbsValueShifted, this.maxValueCF);
        return fftBufferedImage;
      }).thenAcceptAsync(fftBufferedImage -> this.fftKoefImageView.setImage(SwingFXUtils.toFXImage(fftBufferedImage, null)), Platform::runLater)
        .thenRunAsync( () -> startBtn.setDisable(false), Platform::runLater)
        .thenRunAsync( () -> isCalculatingImage.set(false) )
        .exceptionally(exce -> {
            exce.printStackTrace();
            return null;
          });

      BufferedImage imageOut = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); // Erzeuge neues Bild
      for (int i = 0; i < pixelBuffer.length; i++)
      {
        int argb = grayPixelBuffer[i];
        int x = i % width;
        int y = i / width;
        imageOut.setRGB(x, y, argb);
      }

      mainImageView.setImage(SwingFXUtils.toFXImage(imageOut, null));
      mainImageView.setPreserveRatio(true);
      mainImageView.setSmooth(true);
      mainImageView.setCache(true);
    }
    catch (IOException exce)
    {
      exce.printStackTrace();
    }
  }

  // --------- progress handling ---------

  private BorderPane progress;

  private void showProgressIndicator()
  {
    try
    {
      progress = FXMLLoader.load(getClass().getResource("progress.fxml"));
      progress.getStylesheets().add(getClass().getResource("ui.css").toExternalForm());

      this.startBtn.setDisable(true);
      this.fftImageContainer.getChildren().remove(fftImageView);
      this.fftImageContainer.getChildren().remove(fftTruncKoefImageView);
      this.fftImageContainer.getChildren().add(progress);
    }
    catch (IOException exce)
    {
      exce.printStackTrace();
    }
  }

  private Void closeProgressIndicator()
  {
    this.startBtn.setDisable(false);
    if (progress != null)
    {
      this.fftImageContainer.getChildren().remove(progress);
      this.fftImageContainer.getChildren().add(fftImageView);
      this.fftImageContainer.getChildren().add(fftTruncKoefImageView);
      this.progress = null;
    }

    return (Void) null;
  }
}
