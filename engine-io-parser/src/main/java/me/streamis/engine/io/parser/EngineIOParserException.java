package me.streamis.engine.io.parser;

/**
 * Created by stream.
 */
public class EngineIOParserException extends RuntimeException {
  public EngineIOParserException(String message) {
    super(message);
  }

  public EngineIOParserException(String message, Throwable cause) {
    super(message, cause);
  }

  public EngineIOParserException(Throwable cause) {
    super(cause);
  }

  public EngineIOParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
