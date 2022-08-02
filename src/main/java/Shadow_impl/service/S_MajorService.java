package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import Shadow_impl.util.processName;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ParametersAreNonnullByDefault
public class S_MajorService implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select id from major where name = ? and deptid = ?");
            ps.setString(1,name);
            ps.setInt(2,departmentId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                throw new IntegrityViolationException();
            } else {
                ps.close();
                rs.close();
                ps = conn.prepareStatement("select id from department where id = ?", departmentId);
                rs = ps.executeQuery();
                if(!rs.next()) throw new IntegrityViolationException();

                ps.close();
                rs.close();

                executeSQL.update("insert into major(name,deptid) values (?,?)",name,departmentId);
                ps = conn.prepareStatement("select currval('major_seq')");
                rs = ps.executeQuery();
                rs.next();
                int re = rs.getInt(1);
                rs.close();
                ps.close();
                return re;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        executeSQL.update("insert into coursemajor(type,majorid,courseid) values (?,?,?)",true,majorId,courseId);
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        executeSQL.update("insert into coursemajor(type,majorid,courseid) values (?,?,?)",false,majorId,courseId);
    }
}
