package app.ui;

import static app.util.fft.FFT.fft2;
import static app.util.fft.FFT.ifft2;
import static app.util.fft.FFTUtils.createSubMatrix;
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
import java.util.concurrent.ForkJoinPool;
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

/**
 * Controller for the UI
 */
public class UIController implements Initializable
{
  @FXML
  private BorderPane mainWindow;

  @FXML
  private VBox fftImageContainer;
  
  @FXML
  private VBox imageContainer;

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

  // Progress indicator
  private BorderPane progress;


  // Loading flags, controlling async tasks
  private AtomicBoolean isCalculatingImage = new AtomicBoolean(false);
  private AtomicBoolean timeOutExceptionOccured = new AtomicBoolean(false);
  
  // FFT-Variables
  private double[][] fftMatrix;
  private double[][] fftMatrixAbsValue;
  private CompletableFuture<List<Double>> sortedKoefCF;
  private double maxValueCF;
  
//FFT-Worker Pools
 private volatile ForkJoinPool workerPool = null;
 private volatile ForkJoinPool fftWorkerPool1 = null;
 private volatile ForkJoinPool fftWorkerPool2 = null;

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

    if( this.timeOutExceptionOccured.get() )
      return;
    
    this.showProgressIndicatorCalculateReducedImage();
 
    double compressRate = this.qualitySlider.getValue();

    CompletableFuture<double[][]> fftTruncatedMatrix = CompletableFuture.supplyAsync(() -> {
      List<Double> sortedElement = this.sortedKoefCF.join();
      int thresholdPos = (int) (compressRate * sortedElement.size());
      double threshold = sortedElement.get(thresholdPos);
      return FFTUtils.createTruncatedMatrix(this.fftMatrix, threshold);});

    // split task 1 from fftTruncatedMatrix
    CompletableFuture<?> task1 = fftTruncatedMatrix.thenApplyAsync(fftTruncMatrix -> {
      double[][] fftTruncatedMatrixAbsValue = getAbsValuesOfMatrix(fftTruncMatrix);
      double[][] fftTruncatedMatrixAbsValueShifted = shiftAbsFourierKoef(fftTruncatedMatrixAbsValue);
      BufferedImage fftTruncatedImage = getLogScaledBufferedImageForMatix(fftTruncatedMatrixAbsValueShifted, this.maxValueCF);
      return fftTruncatedImage;
    }).thenAcceptAsync(fftTruncatedImage -> this.fftTruncKoefImageView.setImage(SwingFXUtils.toFXImage(fftTruncatedImage, null)), Platform::runLater)
        .orTimeout(20, TimeUnit.SECONDS).exceptionally(exce -> {
          exce.printStackTrace();
          return (Void) null;
        });

    // --- iFFT ---
    Image image = this.mainImageView.getImage();
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();
    
    // Control variable for interrupting the calculation task2
    AtomicBoolean isCancelled = new AtomicBoolean(false);
    
    // split task 2 from fftTruncatedMatrix
    CompletableFuture<?> task2 = fftTruncatedMatrix.thenApplyAsync(fftTruncMatrix -> {
      double[][] ifftReducedImageMatrix = ifft2(fftTruncMatrix);
      if( isCancelled.get() ) { System.err.println("Break after Step 1"); return null; }
      double[][] ifftReducedImageMatrixPadded = createSubMatrix(ifftReducedImageMatrix, width, height);
      if( isCancelled.get() ) { System.err.println("Break after Step 2"); return null; }
      double[][] ifftReducedImageMatrixPaddedAbsValue = getAbsValuesOfMatrix(ifftReducedImageMatrixPadded);
      if( isCancelled.get() ) { System.err.println("Break after Step 3"); return null; }
      
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
    }).thenAcceptAsync(imageOut -> this.fftImageView.setImage(SwingFXUtils.toFXImage(imageOut, null)), Platform::runLater)
        .orTimeout(20, TimeUnit.SECONDS)
        .exceptionally(exce -> {
          isCancelled.set(true);
          exce.printStackTrace();
          return (Void) null;
        });

    // wait for task1 and task2 and finally close indicator
    CompletableFuture.allOf( task1, task2).handleAsync((val,exce) -> closeProgressIndicatorCalculateReducedImage() , Platform::runLater);
  }


  @FXML
  public void exit()
  {
    Platform.exit();
  }
  
  // --- Application initialisation ---

  private CpuInfoPublisher publisher;
  private DecimalFormat df = new DecimalFormat("#0.0");

  @Override
  public void initialize(URL location, ResourceBundle resources)
  {
    // Initialize worker pools
    CompletableFuture<?> initThreadPools = CompletableFuture.runAsync(() -> { 
      int numOfProc = Runtime.getRuntime().availableProcessors();
      
      this.workerPool = new ForkJoinPool( Math.max(1, numOfProc-1) );
      this.fftWorkerPool1 = new ForkJoinPool( Math.max(1, numOfProc/2) );
      this.fftWorkerPool2 = new ForkJoinPool( Math.max(1, numOfProc/2) );
    } );
    
    this.startBtn.setDisable(true);
    this.cpuLoadBar.setFill(Color.GREEN);

    this.qualityValue.textProperty().bind(qualitySlider.valueProperty().asString("%6.2f"));

    try
    {
      progress = FXMLLoader.load(getClass().getResource("progress.fxml"));
      progress.getStylesheets().add(getClass().getResource("ui.css").toExternalForm());
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    
    // Initialize the hardware detection for getting the CPU load
    CompletableFuture.runAsync(() -> {  
      publisher = CpuInfoPublisher.getInstance();
      publisher.subscribe(value -> Platform.runLater(() -> {
        repaintGradient(value);
        cpuLabel.setText(df.format(100 * value) + " %");
      }));
    }).orTimeout(3, TimeUnit.SECONDS).exceptionally(ex -> {
      ex.printStackTrace();
      return (Void) null;
    });
    
    // wait until pools are initilized
    initThreadPools.join();
    assert(this.workerPool != null);
    assert(this.fftWorkerPool1 != null);
    assert(this.fftWorkerPool2 != null);
  }

  // Color gradient for CPU load
  private final Stop[] stops = new Stop[] { new Stop(0, Color.GREEN), new Stop(1, Color.RED) };

  private void repaintGradient(double value)
  {
    LinearGradient linGradient = new LinearGradient(0, 0, 0, 2 * (1 - value) * cpuLoadBar.getHeight(), false, CycleMethod.NO_CYCLE, stops);
    cpuLoadBar.setFill(linGradient);
  }

  
  // Callback-Method:  called if an image is selected in the preview list
  private void loadImageConvertToGrayScaleAndCalculateFourierCoeffiecents(File file)
  {   
    try
    {
      this.startBtn.setDisable(true);
      
      this.timeOutExceptionOccured.set(false);

      byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
      InputStream iStream = new ByteArrayInputStream(bytes);
      Image image = new Image(iStream);

      PixelReader pReader = image.getPixelReader();
      int width = (int) image.getWidth();
      int height = (int) image.getHeight();
      
      System.out.println("Image size " + width + " x " + height + " (" + (width * height) + ") pixels");

      WritablePixelFormat<IntBuffer> format = WritablePixelFormat.getIntArgbInstance();

      int[] pixelBuffer = new int[width * height];
      pReader.getPixels(0, 0, width, height, format, pixelBuffer, 0, width);

      int[] grayPixelBuffer = createNewGrayScaleBuffer(pixelBuffer);
      
      showProgressIndicatorCalculateFFT();
      
     // Control variable for interrupting the calculation task2
      AtomicBoolean isCancelled = new AtomicBoolean(false);
      
      CompletableFuture.supplyAsync(() -> {
        double[][] imageMatrix = getPaddedPowerOf2MatrixForPixelBuffer(grayPixelBuffer, width, height);
        this.fftMatrix = fft2(imageMatrix);
        if( isCancelled.get() ) return null;
        this.fftMatrixAbsValue = getAbsValuesOfMatrix(fftMatrix);
        
        if( isCancelled.get() ) return null;
        this.sortedKoefCF = CompletableFuture.supplyAsync(() -> getSortedElements(fftMatrixAbsValue));

        if( isCancelled.get() ) return null;
        double[][] fftMatrixAbsValueShifted = FFTUtils.shiftAbsFourierKoef(fftMatrixAbsValue);

        this.maxValueCF = this.sortedKoefCF.join().get(this.sortedKoefCF.join().size() - 1);
        if( isCancelled.get() ) return null;
        BufferedImage fftBufferedImage = getLogScaledBufferedImageForMatix(fftMatrixAbsValueShifted, this.maxValueCF);
        return fftBufferedImage;
      }).thenAcceptAsync(fftBufferedImage -> this.fftKoefImageView.setImage(SwingFXUtils.toFXImage(fftBufferedImage, null)), Platform::runLater)
        .thenRunAsync( () -> { this.startBtn.setDisable(false); closeProgressIndicatorCalculateFFT();}, Platform::runLater)
        .thenRunAsync( () -> isCalculatingImage.set(false) )
        .orTimeout(20, TimeUnit.SECONDS )
        .exceptionally(exce -> {
            isCancelled.set(true);
            this.timeOutExceptionOccured.set(true);
            exce.printStackTrace();
            isCalculatingImage.set(false);
            CompletableFuture.runAsync(() -> closeProgressIndicatorCalculateFFT(), Platform::runLater);
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

  // -----------------------------------------------
  // --------- progress indicator handling ---------
  // -----------------------------------------------
  
  private void showProgressIndicatorCalculateFFT()
  {
      this.startBtn.setDisable(true);
      
      this.imageContainer.getChildren().remove( fftKoefImageView );
      this.imageContainer.getChildren().add(this.progress);
  }
  
  private Void closeProgressIndicatorCalculateFFT()
  {
     this.imageContainer.getChildren().add( fftKoefImageView );
     this.imageContainer.getChildren().remove(this.progress);

    return (Void) null;
  }
  
  private void showProgressIndicatorCalculateReducedImage()
  {
      this.startBtn.setDisable(true);
      this.fftImageContainer.getChildren().remove(this.fftImageView);
      this.fftImageContainer.getChildren().remove(this.fftTruncKoefImageView);
      this.fftImageContainer.getChildren().add(this.progress);
  }

  private Void closeProgressIndicatorCalculateReducedImage()
  {
     this.startBtn.setDisable(false);
     this.fftImageContainer.getChildren().remove(this.progress);
     this.fftImageContainer.getChildren().add(this.fftImageView);
     this.fftImageContainer.getChildren().add(this.fftTruncKoefImageView);

    return (Void) null;
  }
}
