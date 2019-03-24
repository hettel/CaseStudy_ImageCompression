package tool.hal;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public abstract class AbstractCpuInfoSubscriber implements Subscriber<Double>
{
  private Subscription subscription;
  
  @Override
  public void onSubscribe(Subscription subscription)
  {
    this.subscription = subscription;
    this.subscription.request(Long.MAX_VALUE);
  }

  @Override
  public void onError(Throwable throwable)
  {
    throwable.printStackTrace();
  }

  @Override
  public void onComplete()
  {
    System.out.println("Publisher completed");
  }

}
