package app.util.fft;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public final class FFTUtils
{
  private FFTUtils()
  {
    
  }
  
  public static double[][] getPaddedPowerOf2MatrixForPixelBuffer(int[] pixelBuffer, int width, int height)
  {
    int adjustWidth = nextPowerOf2(width);
    int adjustHight = 2*nextPowerOf2(height);

    double[][] matrix = new double[adjustWidth][adjustHight];
    for (int i = 0; i < adjustWidth; i++)
    {
      for (int j = 0; j < adjustHight/2; j++)
      {
        if (i < width && j < height)
        {
          matrix[i][2*j] = pixelBuffer[i * height + j] & 0xff;
          matrix[i][2*j + 1] = 0.0;
        }
        else
        {
          matrix[i][2*j] = 0.0;
          matrix[i][2*j+1] = 0.0;
        }
      }
    }
    
    return matrix;
  }
  
  
  public static double[][] nullifyKoefficients(double[][] cMatrix, double threshold)
  {
    int rows = cMatrix.length;
    int cols = cMatrix[0].length;
    
    double[][] result = new double[rows][cols];
    
    double thresholdSquared = threshold*threshold;
    IntStream.range(0, rows).parallel().forEach( i -> {
    {
      for(int j=0; j<cols/2; j++)
      {
        double absSquard = cMatrix[i][2*j]*cMatrix[i][2*j] + cMatrix[i][2*j+1]*cMatrix[i][2*j+1];
        if( absSquard > thresholdSquared )
        {
          result[i][2*j] = cMatrix[i][2*j];
          result[i][2*j+1] = cMatrix[i][2*j+1];
        }
        else
        {
          result[i][2*j] = 0.0;
          result[i][2*j+1] = 0.0;
        }
      }
    }  }); 
    
    return result;
  }
  
  
  public static double[][] createNewMatrixWithNextPowerOfTwoDimensions(double[][] matrix)
  {
    assert (matrix != null && matrix[0] != null);

    int rows = matrix.length;
    int cols = matrix[0].length;

    // check lenght
    int adjustedRows = nextPowerOf2(rows);
    int adjustedCols = 2*nextPowerOf2(cols/2);
    double[][] adjustedPixel = new double[adjustedRows][];

    IntStream.range(0, adjustedRows).parallel().forEach( row -> {
    //for (int row = 0; row < adjustedRows; row++)
    {
      adjustedPixel[row] = new double[adjustedCols];
      if (row < rows)
      {
        for (int col = 0; col < adjustedCols; col++)
        {
          if (col < cols)
          {
            adjustedPixel[row][col] = matrix[row][col];
          }
          else
          {
            adjustedPixel[row][col] = 0;
          }
        }
      }
      else
      {
        for (int col = 0; col < adjustedCols; col++)
        {
          adjustedPixel[row][col] = 0;
        }
      }
    }
    });

    return adjustedPixel;
  }
  
  
  public static double[][] extractSubMatrix(double[][] matrix, int rowSize, int colSize)
  {
    assert( matrix.length >= rowSize && matrix[0].length/2 >= colSize );
    
    double[][] newMatrix = new double[rowSize][2*colSize];
   
    for (int row = 0; row < rowSize; row++)
    {
      for (int col = 0; col < 2*colSize; col++)
      {
        newMatrix[row][col] = matrix[row][col];
      }
    }
    
    return newMatrix;
  }
  
  
  public static double[][] getAbsValuesOfMatrix(double[][] cMatrix)
  {  
    int row = cMatrix.length;
    int col = cMatrix[0].length;
    double[][] dMatrix = new double[row][col/2];
    
    IntStream.range(0, row).parallel().forEach( i -> {
    //for(int i=0; i < row; i++)
    {
      for(int j=0; j<col/2; j++)
      {
        dMatrix[i][j] = Math.sqrt( cMatrix[i][2*j]*cMatrix[i][2*j] + cMatrix[i][2*j+1]*cMatrix[i][2*j+1] );
      }
    }
    });
    
    return dMatrix;
  }
  
  public static int nextPowerOf2(int n)
  {
    if( n == 1 ) return 1;
    if( n == 0 ) return 0;
    
    int p = 1;
    if (n > 0 && (n & (n - 1)) == 0)
      return n;

    while (p < n)
      p <<= 1;

    return p;
  }
  
  public static int[] getPixelBufferFor(double[][] matrix)
  {
    int width = matrix.length;
    int height = matrix[0].length;

    int[] pixelBuffer = new int[width*height];
    for (int i = 0; i < width; i++)
    {
      for (int j = 0; j < height; j++)
      {
        if (matrix[i][j] > 255)
          matrix[i][j] = 255;
        if (matrix[i][j] < 0)
          matrix[i][j] = 0;

        int grayValue = ((int) Math.round(matrix[i][j])) & 0xff;
        pixelBuffer[i * height + j] = (grayValue << 16) + (grayValue << 8) + grayValue;
      }
    }

    pixelBuffer = createNewGrayScaleBuffer(pixelBuffer);

    return pixelBuffer;
  }

  
  public static int[] createNewGrayScaleBuffer(int[] buffer)
  {
    int[] pixelBuffer = new int[buffer.length];
    IntStream.range(0, pixelBuffer.length).parallel().forEach(i -> {
      int rgb = buffer[i];
      int red = (rgb >> 16) & 0xff;
      int green = (rgb >> 8) & 0xff;
      int blue = (rgb >> 0) & 0xff;

      int gray = (int) (red + green + blue) / 3;

      pixelBuffer[i] = (gray << 16) + (gray << 8) + gray;
    });


    return pixelBuffer;
  }

  public static double[][] shiftAbsFourierKoef(double[][] matrix)
  {
    int width = matrix.length;
    int height = matrix[0].length;

    // Change koefficients (fftshif)
    double[][] wscratch = new double[width / 2][];
    System.arraycopy(matrix, 0, wscratch, 0, wscratch.length);
    System.arraycopy(matrix, width / 2, matrix, 0, wscratch.length);
    int wpos = (width % 2 == 0) ? width / 2 : width / 2 + 1;
    System.arraycopy(wscratch, 0, matrix, wpos, wscratch.length);

    double[] hscratch = new double[height / 2];
    for (int i = 0; i < width; i++)
    {
      System.arraycopy(matrix[i], 0, hscratch, 0, hscratch.length);
      System.arraycopy(matrix[i], height / 2, matrix[i], 0, hscratch.length);
      int hpos = (height % 2 == 0) ? height / 2 : height / 2 + 1;
      System.arraycopy(hscratch, 0, matrix[i], hpos, hscratch.length);
    }

    return matrix;
  }


  public static List<Double> getSortedElements(double[][] matrix)
  {
    return Arrays.stream(matrix).flatMapToDouble( row -> Arrays.stream(row) ).sorted().boxed().collect(Collectors.toList());
  }

  public static BufferedImage getLogScaledBufferedImageForMatix(double[][]  matrix, double maxElement)
  {
    int width = matrix.length;
    int height = matrix[0].length;

    double logMax = Math.log(maxElement);

    BufferedImage imageOut = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); // Erzeuge neues Bild
    for (int i = 0; i < width * height; i++)
    {
      int x = i % width;
      int y = i / width;

      int grayValue = (int) Math.round(Math.log(matrix[x][y]) / logMax * 255);
      int rgb = (grayValue << 16) + (grayValue << 8) + grayValue;
      imageOut.setRGB(x, y, rgb);
    }

    return imageOut;
  }
}
