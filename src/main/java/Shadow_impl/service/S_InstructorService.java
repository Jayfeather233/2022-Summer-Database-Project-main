package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import Shadow_impl.util.processName;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ParametersAreNonnullByDefault
public class S_InstructorService implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select id from usert where id = ?");
            ps.setInt(1,userId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                throw new IntegrityViolationException();
            } else {
                ps.close();
                rs.close();

                executeSQL.update("insert into usert(id,type,full_name) values (?,?,?)",
                        userId,'i', processName.getName(firstName,lastName));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
