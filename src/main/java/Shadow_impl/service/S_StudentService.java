package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import Shadow_impl.util.prere_check;
import Shadow_impl.util.processName;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSearchEntry;
import cn.edu.sustech.cs307.dto.CourseTable;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;

@ParametersAreNonnullByDefault
public class S_StudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        if(executeSQL.ifExist("select id from usert where id = ?",userId)) throw new IntegrityViolationException();
        if(!executeSQL.ifExist("select id from major where id = ?",majorId)) throw new IntegrityViolationException();

        executeSQL.update("insert into usert(id, type,full_name) values(?,?,?)",userId,"s", processName.getName(firstName,lastName));
        executeSQL.update("insert into student(id, majorid, enrolleddate) values(?,?,?)",userId, majorId,enrolledDate);
    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        return null;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        if(!executeSQL.ifExist("select id from section where id = ?",sectionId)) return EnrollResult.COURSE_NOT_FOUND;
        if(executeSQL.ifExist("select studentid from enroll where studentid = ? and sectionid = ? and grade is null",studentId,sectionId)) return EnrollResult.ALREADY_ENROLLED;
        int maxScore = 0;
        int courseId = 0;
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select max(grade) from enroll where studentid = ? and sectionid = ?");
            ps.setInt(1,studentId);
            ps.setInt(2,sectionId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                maxScore = rs.getInt(1);
            }
            ps.close();
            rs.close();

            ps = conn.prepareStatement("select courseid from section where id = ?");
            ps.setInt(1,sectionId);
            rs = ps.executeQuery();
            if(rs.next()){
                courseId =rs.getInt(1);
            }
            ps.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if(maxScore >= 60) return EnrollResult.ALREADY_PASSED;

        if(!prere_check.check(studentId,courseId)) return EnrollResult.PREREQUISITES_NOT_FULFILLED;

        return EnrollResult.SUCCESS;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {

    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {

    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        CourseTable ct = new CourseTable();
        for(int i=1;i<=7;i++) ct.table.put(DayOfWeek.of(i),new HashSet<>());
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement(
                    """
                            select dayOfWeek, c.name, section.sectionName, instructorID, classStart, classEnd, location
                            from section
                                join (select id,beginT from semester where ? between begint and endt) s on section.semesterID = s.id
                                join class on section.id = class.sectionID
                                join enroll e on (section.id = e.sectionID and e.studentID = ?)
                                join course c on section.courseID = c.id
                            where getWeekNumber(begint, ?) = any(weekList);""");
            ps.setDate(1,date);
            ps.setInt(2,studentId);
            ps.setDate(3,date);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                CourseTable.CourseTableEntry cte = new CourseTable.CourseTableEntry();
                cte.courseFullName = String.format("%s[%s]",rs.getString(2),rs.getString(3));
                cte.instructor = new Instructor();
                cte.instructor.id = rs.getInt(4);
                cte.classBegin = rs.getShort(5);
                cte.classEnd = rs.getShort(6);
                cte.location = rs.getString(7);
                ct.table.get(DayOfWeek.of(rs.getInt(1))).add(cte);
            }

            rs.close();
            ps.close();
            conn.close();
            return ct;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
