package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class S_SemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        if(begin.after(end)) throw new IntegrityViolationException();
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select name from semester where name = ?");
            ps.setString(1,name);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                throw new IntegrityViolationException();
            } else {
                ps.close();
                rs.close();

                executeSQL.update("insert into semester(name, begint, endt) values(?,?,?)",name,begin,end);
                ps = conn.prepareStatement("select currval('semester_seq')");
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
    public void removeSemester(int semesterId) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select id from section where semesterid = ?");
            ps.setInt(1,semesterId);
            ResultSet rs = ps.executeQuery();
            ArrayList<Integer> al = new ArrayList<>();
            while(rs.next()){
                al.add(rs.getInt(1));
            }

            for(Integer i : al){
                executeSQL.update("delete from class where sectionid = ?", i);
                executeSQL.update("delete from enroll where sectionid = ?", i);
            }

            executeSQL.update("delete from section where semesterid = ?", semesterId);
            executeSQL.update("delete from semester where id = ?", semesterId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        try{
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "select id, name, begint, endt from semester";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            List<Semester> L = new ArrayList<>();
            while(resultSet.next()){
                Semester S = new Semester();
                S.id = resultSet.getInt(1);
                S.name = resultSet.getString(2);
                S.begin = resultSet.getDate(3);
                S.end = resultSet.getDate(4);
                L.add(S);
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
