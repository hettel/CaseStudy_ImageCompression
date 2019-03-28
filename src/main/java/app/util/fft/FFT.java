package app.util.fft;

import java.util.stream.IntStream;

/**
 * Methods for calculating the Fourier Transfomations
 * 
 * The fft() and ifft() methods are an adaption of the algorithm from
 * https://introcs.cs.princeton.edu/java/97data/FFT.java.html
 *
 * Because of memory efficiency complex vectors are represented by double array:
 * [c_1, c_2, ..., c_n] is mapped to [Re(c_1), Im(c_1), Re(c_2), Im(c_2), ..., Re(c_n), Im(c_n)]
 * 
 * Remark: This class should not be used in productive software!
 */
public final class FFT
{
  private FFT()
  {
    
  }
  
  // compute the FFT of x[], assuming its length is a power of 2
  public static double[] fft(double[] x)
  {
    int n = x.length/2;

    // base case
    if (n == 1)
      return new double[] {x[0],x[1]};

    // radix 2 Cooley-Tukey FFT
    if (n % 2 != 0)
    {
      throw new IllegalArgumentException("n is not a power of 2");
    }

    // fft of even terms
    double[] even = new double[n];
    for (int k = 0; k < n / 2; k++)
    {
      even[2*k] = x[4 * k];
      even[2*k+1] = x[4 * k+1];
    }
    double[] q = fft(even);
    
    
    // fft of odd terms
    double[] odd = even; // reuse the array
    for (int k = 0; k < n / 2; k++)
    {
      odd[2*k] = x[4 * k + 2];
      odd[2*k+1] = x[4 * k + 3];
    }
    double[] r = fft(odd);
    
    // combine
    double[] y = new double[2*n];
    for (int k = 0; k < n/2 ; k++)
    {
      double kth = -2 * k * Math.PI / n;
      double real = Math.cos(kth);
      double imag = Math.sin(kth);
      
      double tmpRe = real*r[2*k] - imag*r[2*k +1];
      double tmpIm = real*r[2*k+1] + imag*r[2*k];
          
      y[2*k] = q[2*k] + tmpRe;
      y[2*k+1] = q[2*k+1] + tmpIm;
      
      y[2*k + n] = q[2*k] - tmpRe;
      y[2*k + 1 + n] = q[2*k + 1] - tmpIm;
    }
    
     return y;
  }

  // compute the inverse FFT of x[], assuming its length is a power of 2
  public static double[] ifft(double[] x)
  {
    double[] y = new double[x.length];

    int n = x.length/2;

    // take conjugate
    for (int i = 0; i < n; i++)
    {
      y[2*i]   = x[2*i];
      y[2*i+1] = (-1.0)*x[2*i+1];
    }

    // compute forward FFT
    y = fft(y);

    // take conjugate again
    for (int i = 0; i < n; i++)
    {
      y[2*i]   = y[2*i]/n;
      y[2*i+1] = (-1.0)*y[2*i+1]/n;
    }

    return y;
  }
  
  //compute the two-dimensonal FFT of matrix[][], assuming its width and height are power of 2
  //first compute the fft of every row and subsequently of the columns
  public static double[][] fft2(double[][] matrix)
  {
    assert (matrix != null && matrix[0] != null);

    int rows = matrix.length;
    int cols = matrix[0].length;
    
    assert (2*rows == cols );

    // col-transformation
    double[][] fftRows = new double[rows][];
    IntStream.range(0, rows).parallel().forEach( i -> {
    {
      fftRows[i] = fft(matrix[i]);
    }
    });
    
    
    //sample columns and transform is
    double[][] fftCols = new double[cols/2][];
    IntStream.range(0, cols/2).parallel().forEach( j -> {
    {
      double[] newCol_j = new double[2*rows];
      for (int i = 0; i < rows; i++)
      {
        newCol_j[2*i] = fftRows[i][2*j];
        newCol_j[2*i+1] = fftRows[i][2*j+1];
      }
          
      fftCols[j] = fft(newCol_j);
    }
    });
        
    // build 2-dim fft-transformation matrix
    IntStream.range(0, rows).parallel().forEach( i -> {
    {
      for (int j = 0; j < cols/2; j++)
      {
        fftRows[i][2*j] = fftCols[j][2*i];
        fftRows[i][2*j + 1] = fftCols[j][2*i+1];
      }
    }
    });
   
    return fftRows;
  }
  
  //compute the two-dimensonal inverse FFT of matrix[][], assuming its width and height are power of 2
  public static double[][] ifft2(double[][] matrix)
  {
    assert (matrix != null && matrix[0] != null);

    int rows = matrix.length;
    int cols = matrix[0].length;
    
    assert (2*rows == cols );
    
    // col-transformation
    double[][] fftRows = new double[rows][];
    IntStream.range(0, rows).parallel().forEach( i -> {
    {
      fftRows[i] = ifft(matrix[i]);
    }
    });
    
    
    //sample columns and transform is
    double[][] fftCols = new double[cols/2][];
    IntStream.range(0, cols/2).parallel().forEach( j -> {
    {
      double[] newCol_j = new double[2*rows];
      for (int i = 0; i < rows; i++)
      {
        newCol_j[2*i] = fftRows[i][2*j];
        newCol_j[2*i+1] = fftRows[i][2*j+1];
      }
          
      fftCols[j] = ifft(newCol_j);
    }
    });
        
    // build 2-dim fft-transformation matrix
    IntStream.range(0, rows).parallel().forEach( i -> {
    {
      for (int j = 0; j < cols/2; j++)
      {
        fftRows[i][2*j] = fftCols[j][2*i];
        fftRows[i][2*j + 1] = fftCols[j][2*i+1];
      }
    }
    });
   
    return fftRows;
  }
}
