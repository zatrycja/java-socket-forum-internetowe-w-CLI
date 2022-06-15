import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(5056);

        // running infinite loop for getting client request
        while (true) {
            Socket socket = null;

            try {
                // socket object to receive incoming client requests
                socket = serverSocket.accept();

                System.out.println("Nawiazano polaczenie z : " + socket);

                // obtaining input and out streams
                DataInputStream dataInput = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());

                System.out.println("Tworzenie watku dla nowego klienta");

                // create a new thread object
                Thread thread = new ClientHandler(socket, dataInput, dataOutput);
                thread.start();

            } catch (Exception e) {
                socket.close();
                e.printStackTrace();
            }
        }
    }
}

class ClientHandler extends Thread {
    final DataInputStream dataInput;
    final DataOutputStream dataOutput;
    final Socket clientSocker;

    public ClientHandler(Socket s, DataInputStream dataInput, DataOutputStream dataOutput) {
        this.clientSocker = s;
        this.dataInput = dataInput;
        this.dataOutput = dataOutput;
    }

    @Override
    public void run() {
        String received;

        while (true) {
            try {
                received = dataInput.readUTF();

                if (received.equals("8")) {
                    System.out.println("Client " + this.clientSocker + " sends exit...");
                    System.out.println("Closing this connection.");
                    this.clientSocker.close();
                    System.out.println("Connection closed");
                    break;
                }

                String[] data = received.split(":|#");
                Map<String, String> query = new HashMap<String, String>();
                for (int i = 0; i < data.length; i += 2) {
                    query.put(data[i], data[i + 1]);
//                    System.out.println(data[i]+": "+data[i+1]);
                }


                // Odpowiedź na żądania klienta
                switch (query.get("type")) {

                    case "register":
                        Socket registerSocket = new Socket("localhost", 1111);
                        DataOutputStream registerOutput = new DataOutputStream(registerSocket.getOutputStream());
                        DataInputStream registerInput = new DataInputStream(registerSocket.getInputStream());
                        registerOutput.writeUTF(received);

                        received = registerInput.readUTF();
                        this.dataOutput.writeUTF(received);

                        registerOutput.close();
                        registerInput.close();
                        registerSocket.close();
                        break;

                    case "login":
                        Socket loginSocket = new Socket("localhost", 1212);
                        DataOutputStream loginOutput = new DataOutputStream(loginSocket.getOutputStream());
                        DataInputStream loginInput = new DataInputStream(loginSocket.getInputStream());
                        loginOutput.writeUTF(received);

                        received = loginInput.readUTF();
                        this.dataOutput.writeUTF(received);
                        loginOutput.close();
                        loginInput.close();
                        loginSocket.close();
                        break;

                    case "chat":
                        Socket chatSocket = new Socket("localhost", 1313);
                        DataOutputStream chatOutput = new DataOutputStream(chatSocket.getOutputStream());
                        DataInputStream chatInput = new DataInputStream(chatSocket.getInputStream());
                        chatOutput.writeUTF(received);

                        received = chatInput.readUTF();
                        this.dataOutput.writeUTF(received);

                        chatInput.close();
                        chatOutput.close();
                        chatSocket.close();
                        break;

                    case "table":
                        Socket tableSocket = new Socket("localhost", 1414);
                        DataInputStream tableInput = new DataInputStream(tableSocket.getInputStream());

                        received = tableInput.readUTF();
                        this.dataOutput.writeUTF(received);

                        tableInput.close();
                        tableSocket.close();
                        break;

                    case "file_transfer":
//                        System.out.println(received);
                        Socket fileSocket = new Socket("localhost", 2526);
                        DataOutputStream fileOutput = new DataOutputStream(fileSocket.getOutputStream());
                        DataInputStream fileInput = new DataInputStream(fileSocket.getInputStream());
//                        System.out.println(query.get("offset") +" - > "+query.get("file_length"));
//                        System.out.println(received);
                        fileOutput.writeUTF(received);

                        if (query.get("mode").equals("download")){
                            received = fileInput.readUTF();
//                            System.out.println(received);
                            this.dataOutput.writeUTF(received);
                        }

                        else if (query.get("mode").equals("upload") && query.get("offset").equals(query.get("file_length"))) {
                            received = fileInput.readUTF();
//                            System.out.println(received);
                            this.dataOutput.writeUTF(received);
                        }

                        fileInput.close();
                        fileOutput.close();
                        fileSocket.close();
                        break;

                    case "writeMessage":
                        dataOutput.writeUTF("type:msg#msg:"+query.get("msg"));
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            this.dataInput.close();
            this.dataOutput.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}