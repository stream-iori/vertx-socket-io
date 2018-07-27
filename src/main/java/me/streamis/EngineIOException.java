package me.streamis;

/**
 * Created by stream.
 */
public class EngineIOException extends RuntimeException {
  public EngineIOException(String message) {
    super(message);
  }

  public EngineIOException(String message, Throwable cause) {
    super(message, cause);
  }

  public EngineIOException(Throwable cause) {
    super(cause);
  }

  protected EngineIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
