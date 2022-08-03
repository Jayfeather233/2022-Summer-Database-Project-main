package Shadow_impl.service;

import Shadow_impl.util.executeSQL;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
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
        //System.out.println(courseId + ' ' + courseName + ' ' + credit + ' ' + classHour);

        if(executeSQL.ifExist("select id from course where id = ?", courseId)) throw new IntegrityViolationException();
        executeSQL.update("insert into course(id, name, credit, classhour, grading) values (?,?,?,?,?)",
                                                courseId, courseName,credit, classHour, grading == Course.CourseGrading.HUNDRED_MARK_SCORE);
        if(prerequisite != null){
            List<List<Course>> L = prerequisite.when(new Prerequisite.Cases<>() {
                @Override
                public List<List<Course>> match(AndPrerequisite self) {
                    List<List<Course>> LL = self.terms.get(0).when(this);

                    for(int i=1;i<self.terms.size();i++){
                        List<List<Course>> LLu = self.terms.get(i).when(this);
                        List<List<Course>> LLt = new ArrayList<>();

                        for(List<Course> Lc : LL)
                            for(List<Course> Lc2 : LLu){
                                List<Course> tmp = new ArrayList<>();
                                tmp.addAll(Lc2);
                                tmp.addAll(Lc);
                                LLt.add(tmp);
                            }

                        LL = LLt;
                    }

                    return LL;
                }

                @Override
                public List<List<Course>> match(OrPrerequisite self) {
                    List<List<Course>> LL = new ArrayList<>();
                    for(Prerequisite p : self.terms)
                        LL.addAll(p.when(this));
                    return LL;
                }

                @Override
                public List<List<Course>> match(CoursePrerequisite self) {
                    Course C = new Course();
                    C.id = self.courseID;
                    List<List<Course>> LL = new ArrayList<>();
                    List<Course> L = new ArrayList<>();
                    L.add(C);
                    LL.add(L);
                    return LL;
                }
            });

            for(List<Course> u : L){
                executeSQL.update("insert into prerequisite(courseid, count) values (?,?)",courseId, u.size());
                int groupId = executeSQL.getSeq("prere_seq");
                for(Course uu : u){
                    executeSQL.update("insert into coursegroup (courseid, groupid) values (?,?)",uu.id, groupId);
                }
            }
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        if(!executeSQL.ifExist("select id from course where id = ?",courseId)) throw new IntegrityViolationException();
        if(!executeSQL.ifExist("select id from semester where id = ?",semesterId)) throw new IntegrityViolationException();

        executeSQL.update("insert into section(courseid, semesterid, sectionname, fullcapacity, leftcapacity) values(?,?,?,?,?)",
                                                    courseId, semesterId, sectionName, totalCapacity,totalCapacity);
        return executeSQL.getSeq("section_seq");
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        if(!executeSQL.ifExist("select id from section where id = ?",sectionId)) throw new IntegrityViolationException();

        short [] SS = new short[weekList.size()];
        int i=0;
        for(Short s : weekList) SS[i++]=s;

        executeSQL.update("insert into class(sectionid, instructorid, dayofweek, weeklist, classstart, classend, location) values(?,?,?,?,?,?,?)",
                                sectionId, instructorId, (short)dayOfWeek.getValue(), SS, classStart, classEnd,location);

        return executeSQL.getSeq("class_seq");
    }

    @Override
    public void removeCourse(String courseId) {
        executeSQL.update("delete from coursegroup " +
                "where courseid=? or groupid in (select id from prerequisite where courseid = ?)",courseId,courseId);
        executeSQL.update("delete from prerequisite " +
                "where courseid = ?", courseId);
        executeSQL.update("delete from coursemajor " +
                "where courseid = ?", courseId);
        executeSQL.update("delete from class " +
                "where sectionid in (select id from section where courseid = ?)", courseId);
        executeSQL.update("delete from enroll " +
                "where sectionid in (select id from section where courseid = ?)", courseId);
        executeSQL.update("delete from section " +
                "where courseid = ?", courseId);
        executeSQL.update("delete from course " +
                "where id = ?", courseId);

        executeSQL.update("update prerequisite " +
                "set count = (select count(*) from coursegroup where groupid = id)");
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
            conn.close();
            return L;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
