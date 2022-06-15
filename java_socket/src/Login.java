import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Login {

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        ServerSocket loginSocket = new ServerSocket(1212);

        while (true){
            Socket clientSocket = loginSocket.accept();
            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());

            String received = dataInputStream.readUTF();
            String data[] = received.split(":|#");
            Map<String, String> query = new HashMap<String, String>();
            for (int i = 0; i < data.length; i += 2)
                query.put(data[i], data[i + 1]);

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tsy", "root", "");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select login, haslo from users");

            while (resultSet.next()){
                if (resultSet.getString("login").equals(query.get("user_name"))) {
                    if (resultSet.getString("haslo").equals(query.get("user_password")))
                        dataOutputStream.writeUTF("type:login#state:true#user_name:"+query.get("user_name")+"#msg:Zalogowano.");
                    else
                        dataOutputStream.writeUTF("type:login#state:false#msg:Niepoprawne haslo.");
                }
            }
            dataOutputStream.writeUTF("type:login#state:false#msg:Niepoprawny login.");
            connection.close();
        }
    }
}