import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            Socket socket = new Socket("localhost", 5056);

            DataInputStream dataInput = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
            Base64.Encoder encoder = Base64.getEncoder();
            Base64.Decoder decoder = Base64.getDecoder();

            String protocol;
            Map<String, String> response = new HashMap<String, String>();

            boolean loggedIn = false;
            String login = null;
            boolean download = false;

            // wymiana informacji między klientem a serwerem
            while (true) {
                if (!download) {
                    System.out.print("\n-----------------------------------------------------------------------\n" +
                            "Menu:\n" +
                            "1. Rejestracja \n" +
                            "2. Logowanie \n" +
                            "3. Dodaj posta \n" +
                            "4. Wyświetlanie postów \n" +
                            "5. Pobierz plik \n" +
                            "6. Dodaj plik \n" +
                            "7. Wyloguj się \n" +
                            "8. Zamknij \n" +
                            "man - pomoc" +
                            "\n-----------------------------------------------------------------------\n\nWybierz opcję: ");

                    String tosend = scanner.nextLine();

                    if (tosend.equals("8")) {
                        dataOutput.writeUTF("8");
                        socket.close();
                        break;
                    }

                    switch (tosend) {
                        case "1":
                            if (!loggedIn) {
                                protocol = "type:register";
                                System.out.println("Rejestracja\n*************************** ");
                                System.out.print("Login: ");
                                protocol += "#user_name:" + scanner.nextLine();
                                System.out.print("Haslo: ");
                                protocol += "#user_password:" + scanner.nextLine();

                                dataOutput.writeUTF(protocol);
                            } else
                                dataOutput.writeUTF("type:writeMessage#msg:Wyloguj się aby zarejestrować nowe konto.");
                            break;

                        case "2":
                            if (!loggedIn) {
                                protocol = "type:login";
                                System.out.println("Logowanie\n*************************** ");
                                System.out.print("Login: ");
                                protocol += "#user_name:" + scanner.nextLine();
                                System.out.print("Haslo: ");
                                protocol += "#user_password:" + scanner.nextLine();

                                dataOutput.writeUTF(protocol);
                            } else
                                dataOutput.writeUTF("type:writeMessage#msg:Jesteś już zalogowany jako " + login);

                            break;

                        case "3":
                            if (loggedIn) {
                                protocol = "type:chat";
                                System.out.println("Dodawanie posta\n*************************** ");
                                protocol += "#user_name:" + login;
                                System.out.print("Treść: ");
                                protocol += "#message:" + scanner.nextLine();

                                dataOutput.writeUTF(protocol);
                            } else
                                dataOutput.writeUTF("type:writeMessage#msg:Zaloguj się aby dodać posta.");
                            break;

                        case "4":
                            dataOutput.writeUTF("type:table");
                            break;

                        case "5":
                            if (loggedIn) {
                                protocol = "type:file_transfer#mode:download";
                                protocol += "#user_name:" + login;
                                System.out.print("Podaj nazwę pliku: ");
                                protocol += "#file_name:" + scanner.nextLine();
                                protocol += "#offset:0";
                                dataOutput.writeUTF(protocol);
                            } else
                                dataOutput.writeUTF("type:writeMessage#msg:Zaloguj się aby pobrać plik.");
                            break;

                        case "6":
                            if (loggedIn) {
                                System.out.print("Podaj ścieżkę do pliku: ");
                                String p = scanner.nextLine();

                                try {
                                    Path path = Paths.get(p);
                                    if (!Files.exists(path))
                                        throw new FileNotFoundException();

                                    else {
                                        if (Files.isDirectory(path))
                                            throw new FileNotFoundException();

                                        File file = new File(p);
                                        FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
                                        String fileName = file.getName();

                                        String packet;
                                        int packetSize = 1024*30;                                                   //1024*50 za duże
                                        int fileLength = (int) (Math.ceil(file.length()/packetSize) * packetSize);
                                        long loadedBytes = 0;
                                        long offset = 0;
                                        byte[] fileContentBytes = new byte[packetSize];

                                        protocol = "type:file_transfer#mode:upload#file_name:" + fileName + "#file_length:" + fileLength + "#offset:-1";
                                        dataOutput.writeUTF(protocol);
                                        System.out.println("Dodawanie... proszę czekać...");
                                        while ((loadedBytes = fileInputStream.read(fileContentBytes)) != -1) {
                                            packet = encoder.encodeToString(fileContentBytes);
                                            dataOutput.writeUTF("type:file_transfer#mode:upload#file_name:" + fileName + "#file_length:" + fileLength + "#offset:" + offset + "#file_content:" + packet);
//                                        System.out.println("dlugosc: "+fileLength+"\toffset: "+offset);

                                            clear(fileContentBytes);
                                            offset += loadedBytes;
                                        }
                                        fileInputStream.close();
                                    }
                                } catch (FileNotFoundException e) {
                                    dataOutput.writeUTF("type:writeMessage#msg:Plik o podanej ścieżce nie istnieje! Sprawdź czy podana ścieżka jest poprawna.");
                                }
                            } else
                                dataOutput.writeUTF("type:writeMessage#msg:Zaloguj się aby dodać plik na serwer.");
                            break;


                        case "7":
                            if (loggedIn) {
                                login = null;
                                loggedIn = false;
                                dataOutput.writeUTF("type:writeMessage#msg:Wylogowano.");
                            } else
                                dataOutput.writeUTF("type:writeMessage#msg:Aby sie wylogować trzeba się najpierw zalogować...");
                            break;


                        case "man":
                            dataOutput.writeUTF("type:writeMessage#msg:\n *** INSTRUKCJA OBSŁUGI ***\n\n" +
                                    "1. Rejestracja - podaj login i hasło aby utworzyć nowe konto. UWAGA! Loginy nie mogą się powtarzać!\n" +
                                    "2. Logowanie - podaj login i hasło ZAREJESTROWANEGO użytkownika. Po zalogowaniu możesz dodawać nowe posty i dodawać/pobierać pliki.\n" +
                                    "3. Dodaj posta - podaj treść nowej wiadomości umieszczonej na tablicy.\n" +
                                    "4. Wyświetlanie postów - wypisuje 10 ostatnich wiadomości z tablicy\n" +
                                    "5. Pobierz plik - pobieranie pliku o podanej z klawiatury nazwie do folderu domowego użytkownika.\n" +
                                    "6. Dodaj plik - dodawanie pliku o podanej z klawiatury ścieżce na serwer dyskowy.\n" +
                                    "7. Wyloguj się - wylogowuje aktywnego użytkownika\n" +
                                    "8. Zamknij - kończy działanie programu.");
                            break;

                        default:
                            dataOutput.writeUTF("type:writeMessage#msg:Wybrano błędną opcję!");
                    }
                }
                // Przetwarzanie i wypisywanie otrzymanej wiadomości

                String received = dataInput.readUTF();
                String[] data = received.split(":|#");

                for (int i = 0; i < data.length; i += 2)
                    response.put(data[i], data[i+1]);

                if (response.get("type").equals("login") && response.get("state").equals("true")) {
                        loggedIn = true;
                        login = response.get("user_name");
                }


                if (response.get("type").equals("download")){
                    Path downloadpath = Paths.get("C:\\Users\\Patka\\Documents\\#studia\\IVsemestr\\ts\\java_socket\\user_files\\"+login+"\\" + response.get("file_name"));
                    File downloaded = new File(String.valueOf(downloadpath));
                    if (response.get("offset").equals("-1"))
                        if (downloaded.exists())
                            downloaded.delete();

                    FileOutputStream fileOutputStream = new FileOutputStream(downloaded, true);
                    int offset = Integer.parseInt(response.get("offset"));
                    int fileLength = Integer.parseInt(response.get("file_length"));
                    byte[] fileContentBytes;

//                    System.out.println(fileLength+" - > "+offset);

                    if (!response.get("offset").equals("-1")) {
                        fileContentBytes = decoder.decode(response.get("file_content"));
                        fileOutputStream.write(fileContentBytes);
                    }

                    if (offset == fileLength)
                        download = false;
                    else {
                        download = true;
                        protocol = "type:file_transfer#mode:download#user_name:"+response.get("user_name")+"#file_name:"+response.get("file_name")+"#offset:"+offset;
                        dataOutput.writeUTF(protocol);
                    }

                    fileOutputStream.close();
                }

                System.out.print(response.get("msg"));
            }

            scanner.close();
            dataInput.close();
            dataOutput.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clear(byte[] table) {
        for (int i = 0; i < table.length; i++)
            table[i] = 0;
    }
}