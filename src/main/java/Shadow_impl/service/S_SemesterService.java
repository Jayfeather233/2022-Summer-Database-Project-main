package Shadow_impl.service;

import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.service.SemesterService;

import java.sql.Date;
import java.util.List;

public class S_SemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        return 0;
    }

    @Override
    public void removeSemester(int semesterId) {

    }

    @Override
    public List<Semester> getAllSemesters() {
        return null;
    }
}
