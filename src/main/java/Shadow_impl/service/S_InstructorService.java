package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import Shadow_impl.util.processName;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
@ParametersAreNonnullByDefault
public class S_InstructorService implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        if(executeSQL.ifExist("select id from usert where id = ?", userId)) throw new IntegrityViolationException();

        executeSQL.update("insert into usert(id,type,full_name) values (?,?,?)",
                userId,'i', processName.getName(firstName,lastName));
    }
}
