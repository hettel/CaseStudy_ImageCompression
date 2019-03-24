package test.util.fft;

import static app.util.fft.FFT.fft2;
import static app.util.fft.FFT.ifft2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


public class Test_FFT2
{
  static Stream<Arguments> getSizes()
  {
    return Stream.of(arguments(64), arguments(128), arguments(256), arguments(512));
  }

  @ParameterizedTest
  @MethodSource("getSizes")
  void test_FFT2(int n)
  {
    Complex[][] cMatrix = new Complex[n][n];
    double[][] dMatrix = new double[n][2 * n];

    Random rand = new Random();
    for (int i = 0; i < n; i++)
    {
      for (int j = 0; j < n; j++)
      {
        double real = rand.nextDouble();
        double imag = rand.nextDouble();
        cMatrix[i][j] = new Complex(real, imag);
        dMatrix[i][2 * j] = real;
        dMatrix[i][2 * j + 1] = imag;
      }
    }

    Complex[][] cFftMatrix = FFTReferenceImplementation.fft2(cMatrix);
    double[][] dFftMatrix = fft2(dMatrix);

    for (int i = 0; i < n; i++)
    {
      for (int j = 0; j < n; j++)
      {
        assertTrue(Math.abs(cFftMatrix[i][j].real - dFftMatrix[i][2 * j]) < 0.0001);
        assertTrue(Math.abs(cFftMatrix[i][j].imag - dFftMatrix[i][2 * j + 1]) < 0.0001);
      }
    }
  }
  
  @ParameterizedTest
  @MethodSource("getSizes")
  void test_iFFT2(int n)
  {
    Complex[][] cMatrix = new Complex[n][n];
    double[][] dMatrix = new double[n][2 * n];

    Random rand = new Random();
    for (int i = 0; i < n; i++)
    {
      for (int j = 0; j < n; j++)
      {
        double real = rand.nextDouble();
        double imag = rand.nextDouble();
        cMatrix[i][j] = new Complex(real, imag);
        dMatrix[i][2 * j] = real;
        dMatrix[i][2 * j + 1] = imag;
      }
    }

    Complex[][] cFftMatrix = FFTReferenceImplementation.ifft2(cMatrix);
    double[][] dFftMatrix = ifft2(dMatrix);

    for (int i = 0; i < n; i++)
    {
      for (int j = 0; j < n; j++)
      {
        assertTrue(Math.abs(cFftMatrix[i][j].real - dFftMatrix[i][2 * j]) < 0.0001);
        assertTrue(Math.abs(cFftMatrix[i][j].imag - dFftMatrix[i][2 * j + 1]) < 0.0001);
      }
    }
  }
}
