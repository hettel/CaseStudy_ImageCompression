package app.util.ui;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.scene.image.ImageView;

/**
 * Helper class for parallel IO
 * 
 * Reads the jpg and png files of a directory and produced a List of ImageView-Objects.
 */
public final class FileIOHelper
{ 
  private FileIOHelper()
  {
  }

  // This method should not run very often. So its okay to create each time a 
  // thread pool for loading the images. Don't forget to shutdown the executor!
  public static List<ImageView> loadPreViewImages(File imageFolder)
  {
    File[] listOfFiles = imageFolder.listFiles((File dir, String name) -> name.endsWith(".png") || name.endsWith(".jpg"));
    
    int numOfProcessors = Runtime.getRuntime().availableProcessors();
    int numOfPoolThreads = Math.min( listOfFiles.length , numOfProcessors*16);
    final ExecutorService executor = Executors.newFixedThreadPool(numOfPoolThreads, new ThreadFactory()
    {
      @Override
      public Thread newThread(Runnable task)
      {
        Thread th = new Thread(task);
        th.setDaemon(true);
        return th;
      }
    });;
    
    List<CompletableFuture<ImageView>> imageViewList = Arrays.stream(listOfFiles)
        .map( imageFile  -> CompletableFuture.supplyAsync( () -> createPreviewImage(imageFile), executor ) )
        .map( future -> future.thenApplyAsync( FileIOHelper::createImageView, executor ) )
        .collect( toList() );
       
    List<ImageView> resultList = imageViewList.stream().map(CompletableFuture::join).collect( toList() );
    executor.shutdown();
    
    return resultList;
  }

  private static PreviewImage createPreviewImage(File imageFile)
  {
    try
    {
      byte[] bytes = Files.readAllBytes(Paths.get(imageFile.getAbsolutePath()));
      InputStream iStream = new ByteArrayInputStream(bytes);

      return new PreviewImage(iStream, imageFile);
    }
    catch (IOException exce)
    {
      exce.printStackTrace();
      throw new RuntimeException(exce);
    }
  }

  private static ImageView createImageView(PreviewImage image)
  {
    ImageView imageView = new ImageView(image);
    imageView.setFitHeight(80);
    imageView.setPreserveRatio(true);
    imageView.setSmooth(true);
    imageView.setCache(true);
    return imageView;
  }
}
