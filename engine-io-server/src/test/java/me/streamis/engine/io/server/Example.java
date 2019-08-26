package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by stream.
 */
public class Example {
  private static final Logger logger = LoggerFactory.getLogger(Example.class);

  static {
    System.setProperty(
      "vertx.logger-delegate-factory-class-name",
      "io.vertx.core.logging.SLF4JLogDelegateFactory"
    );
  }
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    Handler<HttpServerRequest> requestHandler = request -> {
      if (request.path().equals("/favicon.ico")) {
        request.response().putHeader("Content-Type", "image/x-icon").end();
      }
      if (request.path().equals("/")) {
        String index = "<html>\n" +
          "    <head>\n" +
          "        <script src=\"https://cdnjs.cloudflare.com/ajax/libs/engine.io-client/3.1.3/engine.io.min.js\"></script>\n" +
          "        <script>\n" +
          "         function testConn(url,transports,jsonp){\n" +
          "             // eio = Socket\n" +
          "             let u = url || \"ws://127.0.0.1:3000\";\n" +
          "             let t = transports || [\"polling\",\"websocket\"];\n" +
          "             let options = {\n" +
          "                 transports: t\n" +
          "             };\n" +
          "             if(jsonp){\n" +
          "                 options.forceJSONP = true;\n" +
          "             }\n" +
          "             let socket = eio(u,options);\n" +
          "             socket.on('open', () => {\n" +
          "                 console.log('receive open packet.') \n" +
          "                 let totals = 10\n" +
          "                 let count = 0\n" +
          "                 socket.on('message', (data) => {\n" +
          "                     console.log(\"MSG#%d => %s\", ++count, data.toString());\n" +
          "                     if(count == totals){\n" +
          "                       alert(totals + \" message success!\");\n" +
          "\t                     socket.close();\n" +
          "                     }\n" +
          "                 });\n" +
          "                 socket.on('close', ()=>{\n" +
          "                     alert('socket close');\n" +
          "\t             });\n" +
          "\t             for(let i=0;i<totals;i++){\n" +
          "                     setTimeout(() => socket.send('hello world!'),100*i);\n" +
          "                 }\n" +
          "             });\n" +
          "         }\n" +
          "        </script>\n" +
          "    </head>\n" +
          "    <body>\n" +
          "        <a href=\"javascript:testConn(null,['polling'])\" role=\"button\">polling</a>\n" +
          "        <br>\n" +
          "        <a href=\"javascript:testConn(null,['polling'],true)\" role=\"button\">jsonp</a>\n" +
          "        <br>\n" +
          "        <a href=\"javascript:testConn(null,['websocket'])\" role=\"button\">websocket</a>\n" +
          "        <br>\n" +
          "        <a href=\"javascript:testConn(null,['polling','websocket'])\" role=\"button\">upgrade</a>\n" +
          "        <br>\n" +
          "    </body>\n" +
          "</html>";
        request.response().putHeader("Content-Type", "text/html").end(index);
      }
    };

    EIOServer engine = new EIOServerImpl(vertx, new EngineOptions().toBuild());
    HttpServer httpServer = vertx.createHttpServer();
    engine.attach(httpServer, requestHandler).connectionHandler(socket -> {
      logger.info("connection success");
      socket.messageHandler(buffer -> {
        logger.info("message received." + buffer.toString());
        socket.send(buffer.toString());
      });
    });
    
    httpServer.listen(3000, event -> logger.info("Listen on 3000"));
  }
}
