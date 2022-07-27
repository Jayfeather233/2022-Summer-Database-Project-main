package Shadow_impl.factory;

import Shadow_impl.service.*;
import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;

import java.util.ArrayList;
import java.util.List;

public class S_ServiceFactory extends ServiceFactory {
    public S_ServiceFactory() {
        super();
        registerService(DepartmentService.class, new S_DepartmentService());
        registerService(CourseService.class, new S_CourseService());
        registerService(InstructorService.class, new S_InstructorService());
        registerService(MajorService.class, new S_MajorService());
        registerService(SemesterService.class, new S_SemesterService());
        registerService(StudentService.class, new S_StudentService());
        registerService(UserService.class, new S_UserService());
    }

    @Override
    public List<String> getUIDs() {
        ArrayList<String> L = new ArrayList<>();
        L.add("12112012");
        return L;
    }
}
