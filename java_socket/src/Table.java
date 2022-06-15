import java.io.*;
import java.net.*;
import java.sql.*;

public class Table {

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        ServerSocket loginSocket = new ServerSocket(1414);
        while (true){
            Socket clientSocket = loginSocket.accept();
            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            String result = "";

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tsy", "root", "");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select p.data, p.tresc, u.login from users u inner join posts p on u.iduser = p.iduser order by p.data desc limit 10");

            while (resultSet.next())
                result += resultSet.getString("login")+" napisał(a) \t\t\t"+resultSet.getString("data")+"\n"+resultSet.getString("tresc")
                        +"\n- - - - - - - - - - - - - - - - - - - - -\n\n";

//            System.out.println(result);
            dataOutputStream.writeUTF("type:table#state:true#msg:"+result);
            connection.close();
        }
    }
}