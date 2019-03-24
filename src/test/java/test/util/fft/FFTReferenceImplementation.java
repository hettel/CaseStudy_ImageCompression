package test.util.fft;


// Methods fft() and ifft() adapted from:
// https://algs4.cs.princeton.edu/code/edu/princeton/cs/algs4/FFT.java.html
public class FFTReferenceImplementation
{
  public static Complex[][] fft2(Complex[][] matrix)
  {
    assert (matrix != null && matrix[0] != null);

    int rows = matrix.length;
    int cols = matrix[0].length;

    Complex[][] tmp1 = new Complex[rows][];
    // col-transformation
    for (int i = 0; i < rows; i++)
    {
      tmp1[i] = fft(matrix[i]);
    }
    
    // row-transformation
    Complex[][] tmp2 = new Complex[cols][];
    for (int j = 0; j < cols; j++)
    {
      Complex[] col_j = new Complex[rows];
      for (int i = 0; i < rows; i++)
      {
        col_j[i] = tmp1[i][j];
      }
     
      tmp2[j] = fft(col_j);
    }

    // build 2-dim fft-transformation matrix
    for (int i = 0; i < rows; i++)
    {
      for (int j = 0; j < cols; j++)
      {
        tmp1[i][j] = tmp2[j][i];
      }
    }
       
    return tmp1;
  }
  
  public static Complex[][] ifft2(Complex[][] matrix)
  {
    assert (matrix != null && matrix[0] != null);

    int rows = matrix.length;
    int cols = matrix[0].length;

 
    Complex[][] tmp1 = new Complex[rows][];
    // col-transformation
    for (int i = 0; i < rows; i++)
    {
      tmp1[i] = ifft(matrix[i]);
    }
    
    // row-transformation
    Complex[][] tmp2 = new Complex[cols][];
    for (int j = 0; j < cols; j++)
    {
      Complex[] col_j = new Complex[rows];
      for (int i = 0; i < rows; i++)
      {
        col_j[i] = tmp1[i][j];
      }

      tmp2[j] = ifft(col_j);
    }

    // build 2-dim fft-transformation matrix
    //Complex[][] f = new Complex[rows][cols];
    for (int i = 0; i < rows; i++)
    {
      for (int j = 0; j < cols; j++)
      {
        tmp1[i][j] = tmp2[j][i];
      }
    }

    return tmp1;
  }
  
  
  // compute the FFT of x[], assuming its length is a power of 2
  public static Complex[] fft(Complex[] x)
  {
    int n = x.length;

    // base case
    if (n == 1)
      return new Complex[] { x[0] };

    // radix 2 Cooley-Tukey FFT
    if (n % 2 != 0)
    {
      throw new IllegalArgumentException("n is not a power of 2");
    }

    // fft of even terms
    Complex[] even = new Complex[n / 2];
    for (int k = 0; k < n / 2; k++)
    {
      even[k] = x[2 * k];
    }
    Complex[] q = fft(even);
    

    // fft of odd terms
    Complex[] odd = even; // reuse the array
    for (int k = 0; k < n / 2; k++)
    {
      odd[k] = x[2 * k + 1];
    }
    Complex[] r = fft(odd);
    
   // combine
    Complex[] y = new Complex[n];
    for (int k = 0; k < n / 2; k++)
    {
      double kth = -2 * k * Math.PI / n;
      Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
      
      y[k] = q[k].add(wk.multiply(r[k]));
      y[k + n / 2] = q[k].subtract(wk.multiply(r[k]));
    }
 
    return y;
  }

  // compute the inverse FFT of x[], assuming its length is a power of 2
  public static Complex[] ifft(Complex[] x)
  {
    int n = x.length;
    Complex[] y = new Complex[n];

    // take conjugate
    for (int i = 0; i < n; i++)
    {
      y[i] = x[i].conjugate();
    }

    // compute forward FFT
    y = fft(y);

    // take conjugate again
    for (int i = 0; i < n; i++)
    {
      y[i] = y[i].conjugate();
    }

    // divide by n
    for (int i = 0; i < n; i++)
    {
      y[i] = y[i].multiply(1.0 / n);
    }

    return y;
  }

}
