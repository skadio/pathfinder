package vrp;

public class Timer {
  private long startTime;
  private long stopTime;
  private boolean running;

  private final double nano = 1000000000.0;

  public Timer() {
    super();
  }

  public void reset() {
    this.startTime = 0;
    this.running = false;
  }

  public void start() {
    this.startTime = System.nanoTime();
    this.running = true;
  }

  public void stop() {
    if (running) {
      this.stopTime = System.nanoTime();
      this.running = false;
    }
  }

  public double getTime() {
    double elapsed;
    if (running) {
      elapsed = ((System.nanoTime() - startTime) / nano);
    } else {
      elapsed = ((stopTime - startTime) / nano);
    }

    return elapsed;
  }
}