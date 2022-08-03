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
        if(executeSQL.ifExist("select name from department where name = ?",name)) throw new IntegrityViolationException();
        executeSQL.update("insert into department(name) values(?)",name);
        return executeSQL.getSeq("department_seq");
    }

    @Override
    public void removeDepartment(int departmentId) {
        //department (id:deptid) major (id:majorid) coursemajor
        // major (id:majorid) student
        executeSQL.update("delete from coursemajor where majorid in (select id from major where deptid = ?)", departmentId);
        executeSQL.update("delete from enroll where studentid in " +
                                    "(select id from student where majorid in " +
                                        "(select id from major where deptid = ?))", departmentId);

        List<Integer> Li = new ArrayList<>();
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select id from student where majorid in (select id from major where deptid = ?)");
            ps.setInt(1,departmentId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                Li.add(rs.getInt(1));
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        executeSQL.update("delete from student where majorid in (select id from major where deptid = ?)", departmentId);
        for(int i : Li){
            executeSQL.update("delete from usert where id = ?",i);
        }

        executeSQL.update("delete from major where deptid = ?",departmentId);
        executeSQL.update("delete from department where id = ?", departmentId);
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
            conn.close();
            return L;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
