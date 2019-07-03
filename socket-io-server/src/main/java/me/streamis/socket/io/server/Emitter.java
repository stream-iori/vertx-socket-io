package me.streamis.socket.io.server;

public interface Emitter {

  Emitter emit(String event, Object... args);

}
