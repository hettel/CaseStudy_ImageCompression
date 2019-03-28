package tool.hal;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

/**
 * Facade for the hardware acces using oshi (Native Operating System 
 * and Hardware Information: https://github.com/oshi/oshi)
 * 
 * Realized as a singleton. The class starts a periodic task reading the cpu load.
 * If the load has changed the new value is published to registered subscribers.
 * 
 * Publisher and Subscriber are reactive streams compatible (http://www.reactive-streams.org/).
 */
public class CpuInfoPublisher implements Publisher<Double>
{
  // Task for reading and publishing the CPU load
  private static class SystemCpuLoadTask implements Runnable
  {
    private final SystemInfo si;
    private final CentralProcessor processor;
    private final SubmissionPublisher<Double> cpuLoadPublisher;
    private double value;

    private SystemCpuLoadTask(SubmissionPublisher<Double> cpuLoadPublisher)
    {
      this.si = new SystemInfo();
      this.processor = si.getHardware().getProcessor();
      ;
      this.cpuLoadPublisher = cpuLoadPublisher;
      this.value = this.processor.getSystemCpuLoadBetweenTicks();
    }

    @Override
    public void run()
    {
      double nextValue = processor.getSystemCpuLoadBetweenTicks();
      if (this.notEquals(value, nextValue))
      {
        cpuLoadPublisher.submit(nextValue);
        value = nextValue;
      }
    }

    private boolean notEquals(double a, double b)
    {
      return Objects.equals(a, b) == false;
    }
  }

  private static CpuInfoPublisher instance = new CpuInfoPublisher();

  public static CpuInfoPublisher getInstance()
  {
    return CpuInfoPublisher.instance;
  }

  private ScheduledExecutorService executor;

  private final int OBSERVATION_PERIOD = 50; // milliseconds
  private SubmissionPublisher<Double> publisher;

  private CpuInfoPublisher()
  {
    this.publisher = new SubmissionPublisher<>();
    executor = Executors.newScheduledThreadPool(1, new ThreadFactory()
    {
      @Override
      public Thread newThread(Runnable task)
      {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        return thread;
      }
    });

    executor.scheduleAtFixedRate(new SystemCpuLoadTask(publisher), 0, OBSERVATION_PERIOD, TimeUnit.MILLISECONDS);
  }

  @Override
  public void subscribe(Subscriber<? super Double> subscriber)
  {
    this.publisher.subscribe(subscriber);
  }

  // Convenience Method used by the application
  public void subscribe(Consumer<? super Double> subscriber)
  {
    publisher.subscribe(new Subscriber<Double>()
    {
      private Subscription subscription;

      @Override
      public void onSubscribe(Subscription subscription)
      {
        this.subscription = subscription;
        this.subscription.request(Long.MAX_VALUE);
      }

      @Override
      public void onNext(Double item)
      {
        subscriber.accept(item);
      }

      @Override
      public void onError(Throwable throwable)
      {
        System.err.println("ERROR occured");
      }

      @Override
      public void onComplete()
      {
        System.out.println("Complete");
      }
    });
  }
}
