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
        //if(executeSQL.ifExist("select name from semester where name = ?", name)) throw new IntegrityViolationException();

        executeSQL.update("insert into semester(name, begint, endt) values(?,?,?)",name,begin,end);
        return executeSQL.getSeq("semester_seq");
    }

    @Override
    public void removeSemester(int semesterId) {
        executeSQL.update("delete from class where sectionid in (select id from section where semesterid = ?)", semesterId);
        executeSQL.update("delete from enroll where sectionid in (select id from section where semesterid = ?)", semesterId);
        executeSQL.update("delete from section where semesterid = ?", semesterId);
        executeSQL.update("delete from semester where id = ?", semesterId);
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
