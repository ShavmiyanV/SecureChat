package server.nio_server;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class NioChatServer {

    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final Charset charset = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 4096;

    public NioChatServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        int port = 5000;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        NioChatServer server = new NioChatServer(port);
        System.out.println("Starting NIO Chat Server on port " + port);
        server.start();
    }

    private void start() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started, waiting for connections...");

        while (true) {
            selector.select(); 
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                try {
                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) handleAccept(key);
                    if (key.isReadable()) handleRead(key);
                    if (key.isWritable()) handleWrite(key);
                } catch (IOException ex) {
                    
                    System.err.println("Connection error: " + ex.getMessage());
                    closeKey(key);
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);

        SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
        ClientAttachment attach = new ClientAttachment();
        clientKey.attach(attach);

        System.out.println("Accepted connection from " + sc.getRemoteAddress());
        
        enqueueMessage(sc, "Welcome to NIO Chat!\n");
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ClientAttachment attach = (ClientAttachment) key.attachment();
        attach.readBuffer.clear();

        int read = sc.read(attach.readBuffer);
        if (read == -1) {
            
            System.out.println("Client closed connection: " + sc.getRemoteAddress());
            closeKey(key);
            return;
        } else if (read == 0) {
            return;
        }

        attach.readBuffer.flip();
        CharBuffer chars = charset.decode(attach.readBuffer);
        attach.incoming.append(chars);

        
        String sb = attach.incoming.toString();
        int idx;
        while ((idx = sb.indexOf('\n')) != -1) {
            String line = sb.substring(0, idx).trim(); 
            sb = sb.substring(idx + 1);
            if (!line.isEmpty()) {
                String msg = "[" + sc.getRemoteAddress() + "] " + line + "\n";
                System.out.print("Received: " + msg); 
                broadcast(msg, sc);
            }
        }
        attach.incoming.setLength(0);
        attach.incoming.append(sb);
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ClientAttachment attach = (ClientAttachment) key.attachment();

        Queue<ByteBuffer> q = attach.writeQueue;

        while (!q.isEmpty()) {
            ByteBuffer buf = q.peek();
            sc.write(buf);
            if (buf.hasRemaining()) {
            
                break;
            }
            q.poll(); 
        }

        if (q.isEmpty()) {
        
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private void broadcast(String message, SocketChannel except) throws IOException {
        byte[] bytes = message.getBytes(charset);
        
        for (SelectionKey key : selector.keys()) {
            if (!key.isValid()) continue;
            Channel channel = key.channel();
            if (channel instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) channel;
                if (sc.equals(except)) continue;
                ClientAttachment attach = (ClientAttachment) key.attachment();
                if (attach != null) {
                    enqueueBytesToAttachment(attach, bytes);
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
            }
        }
    
        selector.wakeup();
    }

    private void enqueueMessage(SocketChannel sc, String msg) throws IOException {
        
        byte[] bytes = msg.getBytes(charset);
        
        SelectionKey key = sc.keyFor(selector);
        if (key == null) return;
        ClientAttachment attach = (ClientAttachment) key.attachment();
        if (attach == null) return;
        enqueueBytesToAttachment(attach, bytes);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    private void enqueueBytesToAttachment(ClientAttachment attach, byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        attach.writeQueue.add(buf);
    }

    private void closeKey(SelectionKey key) {
        try {
            key.cancel();
            Channel ch = key.channel();
            if (ch != null) ch.close();
        } catch (IOException e) {
           
        }
    }

    
    private static class ClientAttachment {
        ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        StringBuilder incoming = new StringBuilder();
        Queue<ByteBuffer> writeQueue = new ArrayDeque<>();
    }
}

