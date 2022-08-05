package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import Shadow_impl.util.check;
import Shadow_impl.util.processName;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@ParametersAreNonnullByDefault
public class S_StudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        if(executeSQL.ifExist("select id from usert where id = ?",userId)) throw new IntegrityViolationException();
        if(!executeSQL.ifExist("select id from major where id = ?",majorId)) throw new IntegrityViolationException();

        executeSQL.update("insert into usert(id, type,first_name,last_name) values(?,?,?,?)",userId,"s", firstName,lastName);
        executeSQL.update("insert into student(id, majorid, enrolleddate) values(?,?,?)",userId, majorId,enrolledDate);
    }

    @Override
    public List<CourseSearchEntry> searchCourse(
            int studentId, int semesterId,
            @Nullable String searchCid, @Nullable String searchName,
            @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
            @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations,
            CourseType searchCourseType,
            boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed,
            boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {

        int majorId = -1;
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select majorid from student where id = ?");
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) majorId = rs.getInt(1);
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        StringBuilder sql = new StringBuilder(
                "select distinct(s.id) as sidd, " +
                        "isConflict(?, s.id) as is_conflict, isPassedPrere(?, c.id) as is_prere " +
                        "from section s " +
                "join course c on s.courseid = c.id " +
                "left join coursemajor cm on c.id = cm.courseid " +
                "join class on s.id = class.sectionid " +
                "join usert u on class.instructorid = u.id " +
                "left join " +
                        "(select e.sectionid, cc.courseid, max(grade) over (partition by courseid) as grade " +
                        "from enroll e " +
                        "join section cc on e.sectionid = cc.id " +
                        "where studentid = ? ) ent on ent.courseid = c.id " +
                "where semesterid = ? ");
        if(searchCid != null) sql.append("and c.id like '%' || ? || '%' ");
        if(searchName != null) sql.append("and (c.name like '%' || ? || '%' " +
                "or s.sectionname like '%' || ? || '%' " +
                "or c.name || '[' || s.sectionname || ']' like '%' || ? || '%') ");
        if(searchInstructor != null) sql.append("and (u.first_name like ? || '%' " +
                "or u.last_name like ? || '%'  " +
                "or u.first_name || u.last_name like ? || '%'  " +
                "or u.first_name || ' ' || u.last_name like ? || '%' ) ");
        if(searchDayOfWeek != null) sql.append("and dayofweek = ? ");
        if(searchClassTime != null) sql.append("and ? between classstart and classend ");

        if(searchClassLocations != null){
            sql.append("and (false ");
            for(String u : searchClassLocations) sql.append("or class.location like '%' || ? || '%' ");
            sql.append(") ");
        }

        switch (searchCourseType){
            case MAJOR_COMPULSORY -> sql.append("and cm.majorid = ? and cm.type = true ");
            case MAJOR_ELECTIVE -> sql.append("and cm.majorid = ? and cm.type = false ");
            case CROSS_MAJOR -> sql.append("and cm.majorid <> ? ");
            case PUBLIC -> sql.append("and cm.majorid is null ");
        }

        if(ignoreFull) sql.append("and s.leftcapacity <> 0 ");
        if(ignorePassed) sql.append("and (ent.grade is null or ent.grade < 60) ");

        sql.insert(0,
                "select c.id cid, c.name cname, c.credit, c.classHour, c.grading, " +
                    "s.id sid, s.sectionname, s.fullcapacity, s.leftcapacity, " +
                    "u.id uid, u.first_name, u.last_name, " +
                    "class.id classid, class.dayofweek, class.weeklist, class.classstart, class.classend, class.location, " +
                    "is_conflict, is_prere " +
                "from ( ");
        sql.append(" ) as tt " +
                "join section s on s.id = tt.sidd " +
                "join course c on s.courseid = c.id " +
                "left join coursemajor cm on c.id = cm.courseid " +
                "join class on s.id = class.sectionid " +
                "join usert u on class.instructorid = u.id " +
                "where true ");

        if(ignoreConflict) sql.append("and is_conflict = false ");
        if(ignoreMissingPrerequisites) sql.append("and is_prere = true ");

        sql.append("order by c.id, c.name, s.sectionname, class.id");
        //System.out.println(sql);


        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int index = 0;
            ps.setInt(++index, studentId);
            ps.setInt(++index, studentId);
            ps.setInt(++index, studentId);
            ps.setInt(++index, semesterId);
            if(searchCid != null) ps.setString(++index, searchCid);
            if(searchName != null){
                ps.setString(++index, searchName);
                ps.setString(++index, searchName);
                ps.setString(++index, searchName);
            }
            if(searchInstructor != null){
                ps.setString(++index, searchInstructor);
                ps.setString(++index, searchInstructor);
                ps.setString(++index, searchInstructor);
                ps.setString(++index, searchInstructor);
            }
            if(searchDayOfWeek != null) ps.setInt(++index, searchDayOfWeek.getValue());
            if(searchClassTime != null) ps.setShort(++index, searchClassTime);

            if(searchClassLocations != null){
                for(String u : searchClassLocations) ps.setString(++index, u);
            }

            if(searchCourseType != CourseType.ALL && searchCourseType != CourseType.PUBLIC) ps.setInt(++index, majorId);

            ResultSet rs = ps.executeQuery();
            List<CourseSearchEntry> Lc = new ArrayList<>();
            while(rs.next()){
                CourseSearchEntry ccc;
                if(Lc.size() == 0) {
                    CourseSearchEntry cse = new CourseSearchEntry();
                    cse.course = new Course();
                    cse.course.id = rs.getString(1);
                    cse.course.name = rs.getString(2);
                    cse.course.credit = rs.getInt(3);
                    cse.course.classHour = rs.getInt(4);
                    cse.course.grading = rs.getBoolean(5) ? Course.CourseGrading.HUNDRED_MARK_SCORE : Course.CourseGrading.PASS_OR_FAIL;

                    cse.section = new CourseSection();
                    cse.section.id = rs.getInt(6);
                    cse.section.name = rs.getString(7);
                    cse.section.totalCapacity = rs.getInt(8);
                    cse.section.leftCapacity = rs.getInt(9);

                    cse.sectionClasses = new HashSet<>();

                    cse.conflictCourseNames = new ArrayList<>();
                    Lc.add(cse);
                } else{
                    ccc = Lc.get(Lc.size() - 1);
                    if(!ccc.course.id.equals(rs.getString(1)) || ccc.section.id != rs.getInt(6)) {
                        CourseSearchEntry cse = new CourseSearchEntry();
                        cse.course = new Course();
                        cse.course.id = rs.getString(1);
                        cse.course.name = rs.getString(2);
                        cse.course.credit = rs.getInt(3);
                        cse.course.classHour = rs.getInt(4);
                        cse.course.grading = rs.getBoolean(5) ? Course.CourseGrading.HUNDRED_MARK_SCORE : Course.CourseGrading.PASS_OR_FAIL;

                        cse.section = new CourseSection();
                        cse.section.id = rs.getInt(6);
                        cse.section.name = rs.getString(7);
                        cse.section.totalCapacity = rs.getInt(8);
                        cse.section.leftCapacity = rs.getInt(9);

                        cse.sectionClasses = new HashSet<>();

                        cse.conflictCourseNames = new ArrayList<>();
                        Lc.add(cse);
                    }
                }
                ccc = Lc.get(Lc.size() - 1);
                CourseSectionClass csc = new CourseSectionClass();
                csc.instructor = new Instructor();
                csc.instructor.id = rs.getInt(10);
                csc.instructor.fullName = processName.getName(rs.getString(11),rs.getString(12));
                csc.id = rs.getInt(13);
                csc.dayOfWeek = DayOfWeek.of(rs.getInt(14));
                csc.weekList = new HashSet<>();
                csc.weekList.addAll(List.of((Short[]) rs.getArray(15).getArray()));
                csc.classBegin = rs.getShort(16);
                csc.classEnd = rs.getShort(17);
                csc.location = rs.getString(18);

                ccc.sectionClasses.add(csc);
            }
            rs.close();
            ps.close();
            int from = pageIndex * pageSize;
            int to = Math.min(Lc.size(),(pageIndex + 1) * pageSize);
            from = Math.min(from,to);

            ps = conn.prepareStatement(
                    "select c.name, s.sectionname " +
                            "from course c " +
                            "join section s on c.id = s.courseid " +
                            "join enroll e on s.id = e.sectionid " +
                            "where e.studentid = ? and isconflictcourse(?, s.id) and semesterid = ? " +
                            //"and (e.grade is null or e.grade >= 60) " +
                            "order by name, sectionname");
            for(int i=from;i<to;i++){
                ps.setInt(1,studentId);
                ps.setInt(2,Lc.get(i).section.id);
                ps.setInt(3,semesterId);
                rs = ps.executeQuery();
                while(rs.next()) Lc.get(i).conflictCourseNames.add(String.format("%s[%s]",rs.getString(1),rs.getString(2)));
                rs.close();
            }
            ps.close();
            conn.close();

            return Lc.subList(from, to);
            //return Lc;
        } catch (SQLException e) {
            System.out.println(sql);
            throw new RuntimeException(e);
        }


    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        if(!executeSQL.ifExist("select id from section where id = ?",sectionId)) return EnrollResult.COURSE_NOT_FOUND;
        if(executeSQL.ifExist("select studentid from enroll where studentid = ? and sectionid = ? and grade is null",studentId,sectionId)) return EnrollResult.ALREADY_ENROLLED;
        int maxScore = 0;
        String courseId = null;
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
                courseId =rs.getString(1);
            }
            ps.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if(maxScore >= 60) return EnrollResult.ALREADY_PASSED;

        if(!check.checkPrere(studentId,courseId)) return EnrollResult.PREREQUISITES_NOT_FULFILLED;

        if(executeSQL.ifExist("""
                select c.id from course c
                    join section s on c.id = s.courseID
                    join (select sectionID from enroll where studentID = ?) ss on ss.sectionID = s.id
                where courseID = ?""", studentId, courseId)) return EnrollResult.COURSE_CONFLICT_FOUND;

        if(check.checkConf(studentId,sectionId)) return EnrollResult.COURSE_CONFLICT_FOUND;

        if(!executeSQL.ifExist("select id from section where id = ? and leftcapacity >= 1",sectionId)) return EnrollResult.COURSE_IS_FULL;

        executeSQL.update("update section set leftcapacity = leftcapacity - 1 where id = ?",sectionId);
        executeSQL.update("insert into enroll(studentid, sectionid) values (?,?)",studentId,sectionId);

        return EnrollResult.SUCCESS;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {

        //if(studentId == 11719232) System.out.println("drop " + sectionId + '\n');
        if(executeSQL.ifExist("select grade from enroll where studentid = ? and sectionid = ? and grade is not null",studentId,sectionId)) throw new IllegalStateException();
        if(!executeSQL.ifExist("select id from section where id = ?",sectionId)) throw new IntegrityViolationException();

        executeSQL.update("delete from enroll where studentid = ? and sectionid = ?",studentId,sectionId);
        executeSQL.update("update section set leftcapacity = leftcapacity + 1 where id = ?",sectionId);
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        if(executeSQL.ifExist("select studentid from enroll where studentid = ? and sectionid = ?",studentId,sectionId)) throw new IntegrityViolationException();

        if(grade == null){
            executeSQL.update("insert into enroll(studentid, sectionid) values(?,?)",studentId,sectionId);
        } else {
            short u = grade.when(new Grade.Cases<>() {
                @Override
                public Short match(PassOrFailGrade self) {
                    if(!executeSQL.ifExist("select c.id from course c join section s on c.id = s.courseid " +
                            "where s.id = ? and c.grading = false",sectionId)) throw new IntegrityViolationException();
                    return (short) (self == PassOrFailGrade.PASS ? 60 : 0);
                }

                @Override
                public Short match(HundredMarkGrade self) {
                    if(!executeSQL.ifExist("select c.id from course c join section s on c.id = s.courseid " +
                            "where s.id = ? and c.grading = true",sectionId)) throw new IntegrityViolationException();
                    return self.mark;
                }
            });
            executeSQL.update("insert into enroll(studentid, sectionid, grade) values(?,?,?)",studentId,sectionId,u);
        }
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        CourseTable ct = new CourseTable();
        ct.table = new HashMap<>();
        for(int i=1;i<=7;i++) ct.table.put(DayOfWeek.of(i),new HashSet<>());
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement(
                    """
                            select dayOfWeek, c.name, section.sectionName, u.id, u.first_name, u.last_name, classStart, classEnd, location
                            from section
                                join (select id,beginT from semester where ? between begint and endt) s on section.semesterID = s.id
                                join class on section.id = class.sectionID
                                join enroll e on (section.id = e.sectionID and e.studentID = ?)
                                join course c on section.courseID = c.id
                                join usert u on u.id = class.instructorid
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
                cte.instructor.fullName = processName.getName(rs.getString(5),rs.getString(6));
                cte.classBegin = rs.getShort(7);
                cte.classEnd = rs.getShort(8);
                cte.location = rs.getString(9);
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
