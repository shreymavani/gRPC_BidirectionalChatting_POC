package server;

import io.grpc.stub.StreamObserver;
import org.proto.Chat;
import org.proto.ChatServiceGrpc;

import java.util.Map;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {

    private Map<StreamObserver<Chat.ChatMessage>, String> connectedClients;
    ChatServiceImpl(Map<StreamObserver<Chat.ChatMessage>, String> connectedClients){
        this.connectedClients = connectedClients;
    }
    @Override
    public StreamObserver<Chat.ChatMessage> chat(StreamObserver<Chat.ChatMessage> responseObserver) {

        connectedClients.put(responseObserver, "");// Add the client to the connected clients list

//        System.out.println("No. Of Alive Connections ="+connectedClients.size());
        System.out.println("Client added "+ responseObserver);

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
                System.err.println("Error in chat stream: " + throwable.getMessage() + responseObserver);
                connectedClients.remove(responseObserver); // Remove the client from the connected clients list
                System.out.println("Remaining No. Of Alive Connections = "+connectedClients.size());

            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                System.out.println("Connection Over "+ responseObserver);
                connectedClients.remove(responseObserver); // Remove the client from the connected clients list
                System.out.println("Remaining No. Of Alive Connections = "+connectedClients.size());
            }
        };
    }
}
