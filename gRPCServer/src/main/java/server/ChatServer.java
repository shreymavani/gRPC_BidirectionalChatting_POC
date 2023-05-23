package server;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.proto.Chat;
import org.proto.ChatServiceGrpc;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private Server server;
    private Map<StreamObserver<Chat.ChatMessage>, String> connectedClients;

    private void start() throws IOException {
        int port = 50051;
        connectedClients = new ConcurrentHashMap<>();

        server = ServerBuilder.forPort(port)
                .addService(new ChatServiceImpl())
//                .addService(ProtoReflectionService.newInstance()) // Enable reflection for the service
                .keepAliveTime(20, TimeUnit.SECONDS) // Time between pings
                .keepAliveTimeout(20, TimeUnit.SECONDS) // Time to wait for ping response
                .permitKeepAliveWithoutCalls(true) // Allow keep-alive pings even if no calls are active
                .build()
                .start();

        System.out.println("Server started on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server");
            ChatServer.this.stop();
            System.out.println("Server shutdown");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
        chatServer.blockUntilShutdown();
    }

    private class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
        @Override
        public StreamObserver<Chat.ChatMessage> chat(StreamObserver<Chat.ChatMessage> responseObserver) {
            connectedClients.put(responseObserver, ""); // Add the client to the connected clients list

            return new StreamObserver<Chat.ChatMessage>() {
                @Override
                public void onNext(Chat.ChatMessage chatMessage) {
                    // Handle incoming message
                    String user = chatMessage.getUser();
                    String message = chatMessage.getMessage();
                    System.out.println("Received message from " + user + ": " + message);

                    // Prepare and send response
                    Chat.ChatMessage response = Chat.ChatMessage.newBuilder()
                                .setUser("Server")
                                .setMessage("Thank you for your message!")
                                .build();
                    responseObserver.onNext(response);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error in chat stream: " + throwable.getMessage());
                    connectedClients.remove(responseObserver); // Remove the client from the connected clients list
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                    System.out.println("Connection Over");
                    connectedClients.remove(responseObserver); // Remove the client from the connected clients list
                }
            };
        }
    }
}
