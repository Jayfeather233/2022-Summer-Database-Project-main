package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class S_DepartmentService implements DepartmentService {
    @Override
    public int addDepartment(String name) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select name from department where name = ?");
            ps.setString(1,name);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                throw new IntegrityViolationException();
            } else {
                ps.close();
                rs.close();

                executeSQL.update("insert into department(name) values(?)",name);
                ps = conn.prepareStatement("select currval('department_seq')");
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
    public void removeDepartment(int departmentId) {
        executeSQL.update("update department set show = false where id = ?", departmentId);
    }

    @Override
    public List<Department> getAllDepartments() {
        try{
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "select id, name from department";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            List<Department> L = new ArrayList<>();
            while(resultSet.next()){
                Department U = new Department();
                U.id = resultSet.getInt(1);
                U.name = resultSet.getString(2);
                L.add(U);
            }
            resultSet.close();
            ps.close();
            return L;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
