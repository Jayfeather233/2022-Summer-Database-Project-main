package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import Shadow_impl.util.processName;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.service.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class S_UserService implements UserService {
    @Override
    public void removeUser(int userId) {
        executeSQL.update("delete from student where id = ?",userId);
        executeSQL.update("delete from class where instructorid = ?",userId);
        executeSQL.update("delete from enroll where studentid = ?",userId);
        executeSQL.update("delete from usert where id = ?",userId);
    }

    @Override
    public List<User> getAllUsers() {
        try{
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "select id, type, first_name, last_name from usert";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            List<User> L = new ArrayList<>();
            while(resultSet.next()){
                if(resultSet.getString(2).equals("s")){
                    User U = new Student();
                    U.fullName = processName.getName(resultSet.getString(3),resultSet.getString(4));
                    U.id = resultSet.getInt(1);
                    L.add(U);
                } else {
                    User U = new Instructor();
                    U.fullName = processName.getName(resultSet.getString(3),resultSet.getString(4));
                    U.id = resultSet.getInt(1);
                    L.add(U);
                }
            }
            resultSet.close();
            ps.close();
            conn.close();
            return L;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
