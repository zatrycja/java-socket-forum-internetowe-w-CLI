import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Chat {

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        ServerSocket chatSocket = new ServerSocket(1313);

        while (true){
            Socket clientSocket = chatSocket.accept();
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
            ResultSet resultSet = statement.executeQuery("select login, iduser from users where login ='"+query.get("user_name")+"'");
            int uid = 0;

            while (resultSet.next())
                if (resultSet.getString("login").equals(query.get("user_name")))
                    uid = resultSet.getInt("iduser");

            PreparedStatement pstmt = connection.prepareStatement("insert into posts values (0, curdate(), ?, ?)");
            pstmt.setInt(1, uid);
            pstmt.setString(2, query.get("message"));
            pstmt.executeUpdate();

            dataOutputStream.writeUTF("type:chat#state:true#msg:Dodano nowego posta.");
            connection.close();
        }
    }
}