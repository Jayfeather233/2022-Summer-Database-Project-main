package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class S_MajorService implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        if(executeSQL.ifExist("select id from major where name = ? and deptid = ?",name,departmentId)) throw new IntegrityViolationException();
        if(!executeSQL.ifExist("select id from department where id = ?", departmentId)) throw new IntegrityViolationException();

        executeSQL.update("insert into major(name,deptid) values (?,?)",name,departmentId);
        return executeSQL.getSeq("major_seq");
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
