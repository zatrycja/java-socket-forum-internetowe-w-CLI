import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Register {

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        ServerSocket registerSocket = new ServerSocket(1111);

        while (true){
            Socket clientSocket = registerSocket.accept();
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
            ResultSet resultSet = statement.executeQuery("select login from users");
            boolean exist = false;

            while (resultSet.next()) {
                if (resultSet.getString("login").equals(query.get("user_name")))
                    exist = true;
            }

            if (!exist) {
                PreparedStatement pstmt = connection.prepareStatement("insert into users values (0, ?, ?)");
                pstmt.setString(1, query.get("user_name"));
                pstmt.setString(2, query.get("user_password"));
                pstmt.executeUpdate();
                new File("C:\\Users\\Patka\\Documents\\#studia\\IVsemestr\\ts\\java_socket\\user_files\\"+query.get("user_name")).mkdirs();
                dataOutputStream.writeUTF("type:register#state:true#msg:Zarejestrowano.");
            }
            else
                dataOutputStream.writeUTF("type:register#state:false#msg:Ten login jest juz zajety.");

            connection.close();
        }
    }
}