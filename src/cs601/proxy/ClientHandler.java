package cs601.proxy;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

	public static final int HTTP = 80;
	protected boolean debug = false;   //debug
    String method;
    String file;
    String HTTPversion;
    Socket browserSocket;
    InputStream      browserIS;
    OutputStream     browserOS;
    Socket proxySocket;
    InputStream      proxyIS;
    OutputStream     proxyOS;

    public ClientHandler (Socket browserSocket) throws IOException {
        this.browserSocket = browserSocket;
        browserIS  = browserSocket.getInputStream();
        browserOS  = browserSocket.getOutputStream();
    }

    public void run(){
        try {
            if( validCommand() ){
                forwardBrowserRequestToRemote();
                forwardRemoteDataToBrowser();
            }
        } catch (IOException e) {
            if(debug) {
                e.printStackTrace();
            }
        } finally {
            try{
                if(browserIS!=null){
                    browserIS.close();
                }
                if(browserOS!=null){
                    browserOS.close();
                }
                if(proxyIS!=null){
                    proxyIS.close();
                }
                if(proxyOS!=null){
                    proxyOS.close();
                }
                if(browserSocket!=null){
                    browserSocket.close();
                }
                if(proxySocket!=null){
                    proxySocket.close();
                }
            }catch (IOException e) {
                if (debug) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean validCommand() throws IOException{
        String command = readLine(browserIS);
        if( command==null || command.equals("")){
            return false;
        }
        else {
            method = command.substring(0,command.indexOf(" "));
            if(method.equals("GET")||method.equals("POST")){
                String command_split[] = command.split(" ");
                String website = command_split[1];                             //http://www.usfca.edu/
                String subsite1 = website.substring(website.indexOf("//")+2);  //www.usfca.edu/
                file = subsite1.substring(subsite1.indexOf("/"));              //    /xxx
                HTTPversion = command_split[2];
                return true;
            }
            else {
                return false;
            }
        }
    }

    public String readLine(InputStream is) throws IOException{
        StringBuilder line = new StringBuilder();
        int c;
        while ( (c=is.read()) != -1 ) {
            line.append((char)c);
            if( c == '\n'){
                return line.toString();
            }
        }
        return null;
    }

    public void forwardBrowserRequestToRemote() throws IOException{
        Map<String,String> headers = getHeaders(browserIS);
        String host = headers.get("host").replace("\r\n","");
        openUpstreamSocket(host);
        makeUpstreamRequest(headers);
    }

    public void forwardRemoteDataToBrowser() throws IOException{
        String response = readLine(proxyIS);  //send HTTP/1.0 200 OK back
        StringBuilder headers = new StringBuilder();      //send headers back with strip
        while (/**response!=null && !response.equals("")*/!response.equals("\r\n")){
            if(!response.toLowerCase().contains("connection") &&
               !response.toLowerCase().contains("keep-alive")){
                headers.append(response);
            }
            response = readLine(proxyIS);
        }
        headers.append("\r\n");
        browserOS.write(headers.toString().getBytes());
        browserOS.flush();
        byte buf[] = new byte[50000];                   //send data back
        int readbyte = 0;
        while(true){
            if(readbyte == -1)
                break;
            readbyte=proxyIS.read(buf, 0, 50000);
            if(readbyte > 0){
                browserOS.write(buf, 0, readbyte);
                browserOS.flush();
            }
        }
//        browserIS.close();
//        browserOS.close();
//        proxyIS.close();
//        proxyOS.close();
    }

    public Map<String,String> getHeaders(InputStream browserIS) throws IOException{
        Map<String,String> headers = new HashMap<String, String>();
        String line = readLine(browserIS);
        while(/*!line.equals("") && line != null &&*/ !line.equals("\r\n")){
            String[] b = line.split(": ");
            if(!b[0].toLowerCase().equals("user-agent") &&
               !b[0].toLowerCase().equals("referer") &&
               !b[0].toLowerCase().equals("proxy-connection") &&
               !(b[0].toLowerCase().equals("connection") && b[1].toLowerCase().equals("keep-alive\r\n"))){
                headers.put(b[0].toLowerCase(),b[1].toLowerCase());
            }
            line = readLine(browserIS);//?
        }
        return headers;
    }

    public void openUpstreamSocket(String host){
        try{
            proxySocket = new Socket(host,HTTP);
            proxyIS = proxySocket.getInputStream();
            proxyOS = proxySocket.getOutputStream();
        }catch (IOException e){
            if (debug){
                e.printStackTrace();
            }
        }

    }

    public void makeUpstreamRequest(Map<String,String> headers) {
        try {
            String request = method + " " + file + " " + "HTTP/1.0\r\n";
            request = request + writeHeaders(headers);
            request += "\r\n";
            if (method.equals("POST")) {
                int len = Integer.parseInt(headers.get("content-length").replace("\r\n", ""));
                StringBuilder postData = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    postData.append((char) browserIS.read());
                }
                request += postData.toString();
                request += "\r\n";
                request += "\r\n";
            }
            proxyOS.write(request.getBytes());
            proxyOS.flush();
        }catch(IOException e){
            if(debug){
                e.printStackTrace();
            }
        }
    }

    public String writeHeaders(Map<String,String> headers){
        StringBuilder sHeaders = new StringBuilder();
        for(String h: headers.keySet()){
            sHeaders.append(h + ": " + headers.get(h));
        }
        return sHeaders.toString();
    }
}
