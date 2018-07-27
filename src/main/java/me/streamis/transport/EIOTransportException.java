package me.streamis.transport;

/**
 * Created by stream.
 */
public class EIOTransportException extends RuntimeException {
  public EIOTransportException(String message) {
    super(message);
  }

  public EIOTransportException(String message, Throwable cause) {
    super(message, cause);
  }

  public EIOTransportException(Throwable cause) {
    super(cause);
  }

  public EIOTransportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
