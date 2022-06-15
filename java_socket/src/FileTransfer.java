import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class FileTransfer {

    public static void main(String[] args) throws IOException {
        ServerSocket fileSocket = new ServerSocket(2526);

        while (true) {
            Socket clientSocket = fileSocket.accept();
            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            Base64.Decoder decoder = Base64.getDecoder();
            Base64.Encoder encoder = Base64.getEncoder();
            byte[] fileContentBytes;

            String received = dataInputStream.readUTF();
            String data[] = received.split(":|#");
            Map<String, String> query = new HashMap<String, String>();
            for (int i = 0; i < data.length; i += 2)
                query.put(data[i], data[i + 1]);

            if (query.get("mode").equals("download")) {
                try {
                    String p = "C:\\Users\\Patka\\Documents\\#studia\\IVsemestr\\ts\\java_socket\\file_server\\" + query.get("file_name");
                    Path path = Paths.get(p);
                    if (!Files.exists(path))
                        throw new FileNotFoundException();

                    else {
                        if (Files.isDirectory(path))
                            throw new FileNotFoundException();

                        File file = new File(p);
                        FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
                        String fileName = file.getName();

                        int packetSize = 1024*30;                                                   //1024*50 za duże
                        String packet;
                        int fileLength = (int)(Math.ceil(file.length()/packetSize)*packetSize);     //ile pakietów trzeba wysłać
                        long loadedBytes = 0;
                        long offset = 0;
                        fileContentBytes = new byte[packetSize];


                            while ((loadedBytes = fileInputStream.read(fileContentBytes)) != -1) {
                                packet = encoder.encodeToString(fileContentBytes);
                                if ((Long.parseLong(query.get("offset"))) == offset) {
                                    System.out.println("dlugosc: " + fileLength + "\toffset: " + offset+" "+query.get("offset"));
                                    offset += packetSize;
                                    if (offset == fileLength)
                                        dataOutputStream.writeUTF("type:download#file_name:" + fileName + "#file_length:" + fileLength + "#offset:" + offset
                                                + "#file_content:" + packet + "#msg:Pobrano plik do katalogu użytkownika "+query.get("user_name"));
                                    else
                                        dataOutputStream.writeUTF("type:download#file_name:" + fileName + "#file_length:" + fileLength + "#offset:" + offset
                                                + "#file_content:" + packet + "#msg:...\n");
                                    dataOutputStream.flush();
                                    break;
                                }
                                clear(fileContentBytes);
                                offset += loadedBytes;
                            }

                        fileInputStream.close();
                    }
                } catch (FileNotFoundException e) {
                    dataOutputStream.writeUTF("type:download#state:false#msg:PLIK o podanej nazwie nie istnieje!");
                }
            } else {
                Path uploadpath = Paths.get("C:\\Users\\Patka\\Documents\\#studia\\IVsemestr\\ts\\java_socket\\file_server\\" + query.get("file_name"));
                File uploaded = new File(String.valueOf(uploadpath));
                if (query.get("offset").equals("-1"))
                    if (uploaded.exists())
                        uploaded.delete();

                FileOutputStream fileOutputStream = new FileOutputStream(uploaded, true);
                int offset = Integer.parseInt(query.get("offset"));
                int fileLength = Integer.parseInt(query.get("file_length"));

                System.out.println(fileLength+" - > "+offset);

                if (!query.get("offset").equals("-1")) {
                    fileContentBytes = decoder.decode(query.get("file_content"));
                    fileOutputStream.write(fileContentBytes);
                }

                if (offset == fileLength) {
                    System.out.println("KONIEC");
                    dataOutputStream.writeUTF("type:upload#state:true#msg:Pomyślnie dodano plik na serwer.");
                }
                fileOutputStream.close();
            }
        }
    }
    public static void clear(byte[] table) {
        for (int i = 0; i < table.length; i++)
            table[i] = 0;
    }
}
