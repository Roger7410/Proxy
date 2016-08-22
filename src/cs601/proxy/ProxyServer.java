package cs601.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {
	public static final boolean SingleThreaded = false;
    public static final int proxyPort = 8080;
	public static void main(String[] args) throws IOException {
        ServerSocket proxyServer = new ServerSocket(proxyPort);
        while (true){
            Socket browserSocket = proxyServer.accept();
            ClientHandler clientHandler = new ClientHandler(browserSocket);
            if(SingleThreaded){
                clientHandler.run();
            }
            else {
                Thread t = new Thread(clientHandler);
                t.start();
            }
        }
    }
}
