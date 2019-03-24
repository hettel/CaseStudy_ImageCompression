package test.util.fft;

import static app.util.fft.FFTUtils.nextPowerOf2;
import static app.util.fft.FFTUtils.getPaddedPowerOf2MatrixForPixelBuffer;
import static app.util.fft.FFTUtils.createNewMatrixWithNextPowerOfTwoDimensions;
import static app.util.fft.FFTUtils.extractSubMatrix;
import static app.util.fft.FFTUtils.getAbsValuesOfMatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


public class Test_FFTUtils
{ 
  static Stream<Arguments> getSizes()
  {
    return Stream.of(arguments(64, 32), arguments(128, 23), arguments(12, 16), arguments(42, 13));
  }

  
  @ParameterizedTest
  @MethodSource("getSizes")
  public void test_getPaddedPowerOf2MatrixForPixelBuffer(int row, int col)
  {
    int[] pixelBuffer = new int[row*col];
    Arrays.setAll(pixelBuffer, i -> ThreadLocalRandom.current().nextInt() );
   
    double[][] cMatrix = getPaddedPowerOf2MatrixForPixelBuffer(pixelBuffer, row, col);
    
    assertEquals( nextPowerOf2(row), cMatrix.length);
    assertEquals(2 * nextPowerOf2(col), cMatrix[0].length);
    
    for (int i = 0; i < cMatrix.length; i++)
    {
      for (int j = 0; j < cMatrix[0].length/2; j++)
      {
        if (j < col && i < row)
        {
          assertTrue(Math.abs(cMatrix[i][2*j] - (pixelBuffer[i*col +j]& 0xff) ) < 0.0001);
        }
        else
        {
          assertTrue(Math.abs(cMatrix[i][2*j]) < 0.0001);
        }
      }
    }
  }
  
  @ParameterizedTest
  @MethodSource("getSizes")
  public void test_extractSubMatrix(int row, int col)
  {
    double[][] cMatrix = new double[row][2 * col];

    Random rand = new Random();
    for (int i = 0; i < row; i++)
    {
      for (int j = 0; j < col; j++)
      {
        double real = rand.nextDouble();
        double imag = rand.nextDouble();
        cMatrix[i][2 * j] = real;
        cMatrix[i][2 * j + 1] = imag;
      }
    }

    double[][] powerOf2Matrix = createNewMatrixWithNextPowerOfTwoDimensions(cMatrix);

    double[][] extractedMatrix = extractSubMatrix(powerOf2Matrix, row, col);

    assertEquals(row, extractedMatrix.length);
    assertEquals(2 * col, extractedMatrix[0].length);

    for (int i = 0; i < row; i++)
    {
      for (int j = 0; j < col; j++)
      {
        assertTrue(Math.abs(extractedMatrix[i][j] - cMatrix[i][j]) < 0.0001);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getSizes")
  public void test_createNewMatrixWithNextPowerOfTwoDimensions(int row, int col)
  {
    double[][] cMatrix = new double[row][2 * col];

    Random rand = new Random();
    for (int i = 0; i < row; i++)
    {
      for (int j = 0; j < col; j++)
      {
        double real = rand.nextDouble();
        double imag = rand.nextDouble();
        cMatrix[i][2 * j] = real;
        cMatrix[i][2 * j + 1] = imag;
      }
    }

    double[][] powerOf2Matrix = createNewMatrixWithNextPowerOfTwoDimensions(cMatrix);
    assertEquals(nextPowerOf2(row), powerOf2Matrix.length);
    assertEquals(2 * nextPowerOf2(col), powerOf2Matrix[0].length);

    for (int i = 0; i < nextPowerOf2(row); i++)
    {
      for (int j = 0; j < 2 * nextPowerOf2(col); j++)
      {
        if (j < 2 * col && i < row)
        {
          assertTrue(Math.abs(powerOf2Matrix[i][j] - cMatrix[i][j]) < 0.0001);
        }
        else
        {
          assertTrue(Math.abs(powerOf2Matrix[i][j]) < 0.0001);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getSizes")
  public void test_getAbsValuesOfMatrix(int row, int col)
  {
    double[][] cMatrix = new double[row][2 * col];

    Random rand = new Random();
    for (int i = 0; i < row; i++)
    {
      for (int j = 0; j < col; j++)
      {
        double real = rand.nextDouble();
        double imag = rand.nextDouble();
        cMatrix[i][2 * j] = real;
        cMatrix[i][2 * j + 1] = imag;
      }
    }

    double[][] absMatrix = getAbsValuesOfMatrix(cMatrix);

    assertEquals(row, absMatrix.length);
    assertEquals(col, absMatrix[0].length);

    for (int i = 0; i < row; i++)
    {
      for (int j = 0; j < col; j++)
      {
        double abs = Math.sqrt(cMatrix[i][2 * j] * cMatrix[i][2 * j] + cMatrix[i][2 * j + 1] * cMatrix[i][2 * j + 1]);
        assertTrue(Math.abs(absMatrix[i][j] - abs) < 0.0001);
      }
    }
  }

  @Test
  public void test_NextPowerOf2()
  {
    assertEquals(16, nextPowerOf2(15));
    assertEquals(16, nextPowerOf2(16));
    assertEquals(32, nextPowerOf2(17));

    // Special cases
    assertEquals(2, nextPowerOf2(2));
    assertEquals(1, nextPowerOf2(1));
    assertEquals(0, nextPowerOf2(0));
  }
}
