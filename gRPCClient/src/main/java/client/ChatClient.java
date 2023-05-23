package client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.proto.Chat;
import org.proto.ChatServiceGrpc;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ChatClient {
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub chatServiceStub;

    private ChatClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // For testing purposes only
                .build();
        chatServiceStub = ChatServiceGrpc.newStub(channel);
    }

    private void chat() {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<Chat.ChatMessage> requestObserver = chatServiceStub.chat(new StreamObserver<Chat.ChatMessage>() {
            @Override
            public void onNext(Chat.ChatMessage chatMessage) {
                // Handle incoming message
                String user = chatMessage.getUser();
                String message = chatMessage.getMessage();
                System.out.println("Received message from " + user + ": " + message);
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error in chat stream: " + throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Chat completed");
                latch.countDown();
            }
        });

        // Read and send user input
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter your message (or 'q' to quit): ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("q")) {
                break;
            }

            Chat.ChatMessage message = Chat.ChatMessage.newBuilder()
                    .setUser("Client")
                    .setMessage(input)
                    .build();
            requestObserver.onNext(message);
        }

        requestObserver.onCompleted();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException {
        ChatClient chatClient = new ChatClient("localhost", 50051);
        chatClient.chat();
        chatClient.shutdown();
    }
}
