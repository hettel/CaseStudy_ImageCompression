package test.util.fft;

import static app.util.fft.FFT.fft;
import static app.util.fft.FFT.ifft;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;



public class Test_FFT
{
  @Test
  void test_4dim_Vector()
  {
    Complex[] cVec = new Complex[4];
    double[]  dVec = new double[2*4];
    
    Random rand = new Random();
    for(int i=0; i < 4; i++)
    {
      double re = rand.nextDouble();
      double im = rand.nextDouble();
      
      cVec[i] = new Complex(re, im);
      dVec[2*i] = re;
      dVec[2*i + 1] = im;
    }
       
    for(int i=0; i < 4; i++)
    {
      assertTrue( Math.abs( cVec[i].real - dVec[2*i]) < 0.0001 );
      assertTrue( Math.abs( cVec[i].imag - dVec[2*i+1]) < 0.0001 );
    }
  }
  
  @Test
  void test_2dim_Vector_fft()
  {
    Complex[] cVec = new Complex[2];
    double[]  dVec = new double[4];
    
    Random rand = new Random();
    for(int i=0; i < 2; i++)
    {
      double re = rand.nextDouble();
      double im = rand.nextDouble();
      
      cVec[i] = new Complex(re, im);
      dVec[2*i] = re;
      dVec[2*i + 1] = im;
    }
    
    Complex[] cFft = FFTReferenceImplementation.fft(cVec);  
    double[] dFft = fft(dVec);
    
    for(int i=0; i < 2; i++)
    {
      assertTrue( Math.abs( cFft[i].real - dFft[2*i]) < 0.0001 );
      assertTrue( Math.abs( cFft[i].imag - dFft[2*i+1]) < 0.0001 );
    }
  }

  static Stream<Arguments> getSizes() {
    return Stream.of(
        arguments(64),
        arguments(128),
        arguments(512),
        arguments(1024),
        arguments(2048)
    );
}
 
  @ParameterizedTest
  @MethodSource("getSizes")
  public void test_fft(int n)
  {
    Complex[] cVec = new Complex[n];
    double[]  dVec = new double[2*n];
    
    Random rand = new Random();
    for(int i=0; i < n; i++)
    {
      double re = rand.nextDouble();
      double im = rand.nextDouble();
      
      cVec[i] = new Complex(re, im);
      dVec[2*i] = re;
      dVec[2*i + 1] = im;
    }
    
    Complex[] cFft = FFTReferenceImplementation.fft(cVec);   
    double[] dFft = fft(dVec);
      
    for(int i=0; i < n; i++)
    {
      assertTrue( Math.abs( cFft[i].real - dFft[2*i]) < 0.0001 );
      assertTrue( Math.abs( cFft[i].imag - dFft[2*i+1]) < 0.0001 );
    }
  }
  
  @ParameterizedTest
  @MethodSource("getSizes")
  public void test_ifft(int n)
  {
    Complex[] cVec = new Complex[n];
    double[]  dVec = new double[2*n];
    
    Random rand = new Random();
    for(int i=0; i < n; i++)
    {
      double re = rand.nextDouble();
      double im = rand.nextDouble();
      
      cVec[i] = new Complex(re, im);
      dVec[2*i] = re;
      dVec[2*i + 1] = im;
    }
    
    Complex[] cFft = FFTReferenceImplementation.fft(cVec);
    Complex[] ciFft = FFTReferenceImplementation.ifft(cFft);
       
    double[] dFft = fft(dVec);
    double[] idFft = ifft(dFft);
    
   
    for(int i=0; i < n; i++)
    {
      assertTrue( Math.abs( ciFft[i].real - idFft[2*i]) < 0.0001 );
      assertTrue( Math.abs( ciFft[i].imag - idFft[2*i+1]) < 0.0001 );
      assertTrue( Math.abs( cVec[i].real - ciFft[i].real) < 0.0001 );
      assertTrue( Math.abs( cVec[i].imag - ciFft[i].imag) < 0.0001 );
      assertTrue( Math.abs( dVec[2*i] - idFft[2*i]) < 0.0001 );
      assertTrue( Math.abs( dVec[2*i+1] - idFft[2*i+1]) < 0.0001 );
    }
  }

}
