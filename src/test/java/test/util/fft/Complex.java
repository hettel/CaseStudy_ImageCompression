package test.util.fft;

public class Complex
{
  public final double real;
  public final double imag;
  
  public Complex(double real, double imag)
  {
    this.real = real;
    this.imag = imag;
  }
  
  public Complex conjugate()
  {
    return new Complex(real, -imag);
  }
  
  public Complex add(Complex c)
  {
    return new Complex( c.real + this.real, c.imag + this.imag);
  }
  
  public Complex subtract(Complex c)
  {
    return new Complex( this.real - c.real, this.imag - c.imag);
  }
  
  public Complex multiply(double real)
  {
    return new Complex( real*this.real, real*this.imag);
  }
  
  public Complex multiply(Complex c)
  {
    return new Complex( this.real*c.real - this.imag*c.imag, this.real*c.imag + this.imag*c.real);
  }
  
  public double abs()
  {
    return Math.sqrt( this.real*this.real + this.imag*this.imag);
  }
  
  @Override
  public String toString()
  {
    return "(" + this.real + " + i" + this.imag + ")";
  }
}
