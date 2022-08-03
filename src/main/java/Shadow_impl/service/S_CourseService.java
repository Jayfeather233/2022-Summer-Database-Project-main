package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class S_CourseService implements CourseService {
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        if(!executeSQL.ifExist("select id from course where id = ?",courseId)) throw new IntegrityViolationException();
        if(!executeSQL.ifExist("select id from semester where id = ?",semesterId)) throw new IntegrityViolationException();

        executeSQL.update("insert into section(courseid, semesterid, sectionname, fullcapacity, leftcapacity)",
                                                    courseId, semesterId, sectionName, totalCapacity,totalCapacity);
        return executeSQL.getSeq("section_seq");
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        if(!executeSQL.ifExist("select id from section where id = ?",sectionId)) throw new IntegrityViolationException();

        executeSQL.update("insert into class(sectionid, instructorid, dayofweek, weeklist, classstart, classend, location)",
                                sectionId, instructorId, (short)dayOfWeek.getValue(), weekList, classStart, classEnd,location);

        return executeSQL.getSeq("class_seq");
    }

    @Override
    public void removeCourse(String courseId) {
    }

    @Override
    public List<Course> getAllCourses() {
        try{
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "select id, name, credit, classhour, grading from course";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            List<Course> L = new ArrayList<>();
            while(resultSet.next()){
                Course U = new Course();
                U.id = resultSet.getString(1);
                U.name = resultSet.getString(2);
                U.credit = resultSet.getInt(3);
                U.classHour = resultSet.getInt(4);
                U.grading = resultSet.getBoolean(5) ? Course.CourseGrading.HUNDRED_MARK_SCORE : Course.CourseGrading.PASS_OR_FAIL;
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
