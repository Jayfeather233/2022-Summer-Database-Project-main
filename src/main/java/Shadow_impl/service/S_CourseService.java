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
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select id from course where id = ?");
            ps.setString(1, courseId);
            ResultSet rs = ps.executeQuery();
            if(!rs.next()) throw new IntegrityViolationException();
            ps.close();
            rs.close();

            ps = conn.prepareStatement("select id from semester where id = ?");
            ps.setInt(1, semesterId);
            rs = ps.executeQuery();
            if(!rs.next()) throw new IntegrityViolationException();
            ps.close();
            rs.close();

            executeSQL.update("insert into section(courseid, semesterid, sectionname, fullcapacity, leftcapacity)",
                    courseId, semesterId, sectionName, totalCapacity,totalCapacity);
            ps = conn.prepareStatement("select currval('section_seq')");
            rs = ps.executeQuery();
            rs.next();
            int re = rs.getInt(1);
            rs.close();
            ps.close();
            return re;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        return 0;
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
