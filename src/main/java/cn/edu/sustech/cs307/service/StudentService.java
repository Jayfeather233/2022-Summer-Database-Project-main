package cn.edu.sustech.cs307.service;

import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 *
 */
@ParametersAreNonnullByDefault
public interface StudentService {
    /**
     * The priority of EnrollResult should be (if not SUCCESS):
     *
     * COURSE_NOT_FOUND > ALREADY_ENROLLED > ALREADY_PASSED > PREREQUISITES_NOT_FULFILLED >
     * COURSE_CONFLICT_FOUND > COURSE_IS_FULL > UNKNOWN_ERROR
     */
    enum EnrollResult {
        /**
         * Enrolled successfully
         */
        SUCCESS,
        /**
         * Cannot found the course section
         */
        COURSE_NOT_FOUND,
        /**
         * The course section is full
         */
        COURSE_IS_FULL,
        /**
         * The course section is already enrolled by the student
         */
        ALREADY_ENROLLED,
        /**
         * The course (of the section) is already passed by the student
         */
        ALREADY_PASSED,
        /**
         * The student misses prerequisites for the course
         */
        PREREQUISITES_NOT_FULFILLED,
        /**
         * The student's enrolled courses has time conflicts with the section,
         * or has course conflicts (same course) with the section.
         */
        COURSE_CONFLICT_FOUND,
        /**
         * Other (unknown) errors
         */
        UNKNOWN_ERROR
    }

    enum CourseType {
        /**
         * All courses
         */
        ALL,
        /**
         * Courses in compulsory courses of the student's major
         */
        MAJOR_COMPULSORY,
        /**
         * Courses in elective courses of the student's major
         */
        MAJOR_ELECTIVE,
        /**
         * Courses only in other majors than the student's major
         */
        CROSS_MAJOR,
        /**
         * Courses not belong to any major's requirements
         */
        PUBLIC
    }

    /**
     * Add one student according to following parameters.
     * If some of parameters are invalid, throw {@link cn.edu.sustech.cs307.exception.IntegrityViolationException}
     *
     * @param userId
     * @param majorId
     * @param firstName
     * @param lastName
     * @param enrolledDate
     */
    void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate);



    /**
     * 搜索指定学生在该学期的可用课程（'section'），并附加条件。
     * 结果应首先按课程ID排序，然后按课程全名（course.name[section.name]）排序。
     * 忽略所有没有子类的课程部分。
     * 注意：所有ignore*参数都是关于结果是否应该忽略这种情况。
     *即当ignoreFull为真时，结果应该过滤掉所有已满的部分。
     *
     * @param studentId
     * @param semesterId
     * @param searchCid 搜索课程ID。规则：searchCid在course.id中
     * @param searchName 搜索课程名称。规则：searchName在 "course.name[section.name]"中。
     * @param searchInstructor 搜索教员的名字。
     *规则：firstName + lastName以searchInstructor开头。
     *或者firstName + '' + lastName以searchInstructor开始
     *或者firstName以searchInstructor开始
     *或姓以搜索教师开始。
     *param searchDayOfWeek 搜索一周的日子。匹配*任何*在搜索星期的部分的班级。
     * @param searchClassTime 搜索上课时间。匹配本节中任何一个包含搜索班级时间的班级。
     * @param searchClassLocations 搜索班级地点。匹配本节中的任何一个班级，包含搜索班级位置中的任何一个位置。
     * @param searchCourseType 搜索课程类型。参见{@link cn.edu.sustech.cs307.service.StudentService.CourseType}。
     * @param ignoreFull 是否要忽略完整的课程部分。
     * @param ignoreConflict 是否忽略课程或时间冲突的课程部分。
     * 请注意，一个部分既是课程也是时间与自己冲突的。
     * 参见{@link cn.edu.sustech.cs307.dto.CourseSearchEntry#conflictCourseNames}。
     * @param ignorePassed 是否要忽略学生通过的课程。
     * @param ignoreMissingPrerequisites 是否忽略缺少先决条件的课程。
     * @param pageSize 页面大小，实际上是 "limit pageSize"。
     * 它是{@link cn.edu.sustech.cs307.dto.CourseSearchEntry}的数目。
     * @param pageIndex 是页面索引，实际上是 "offset pageIndex * pageSize"。
     *如果页面索引太大，以至于没有消息，则返回一个空列表。
     *返回一个搜索条目的列表。参见 {@link cn.edu.sustech.cs307.dto.CourseSearchEntry}。
     */

    /**
     * Search available courses (' sections) for the specified student in the semester with extra conditions.
     * The result should be first sorted by course ID, and then sorted by course full name (course.name[section.name]).
     * Ignore all course sections that have no sub-classes.
     * Note: All ignore* arguments are about whether or not the result should ignore such cases.
     * i.e. when ignoreFull is true, the result should filter out all sections that are full.
     *
     * @param studentId
     * @param semesterId
     * @param searchCid                  search course id. Rule: searchCid in course.id
     * @param searchName                 search course name. Rule: searchName in "course.name[section.name]"
     * @param searchInstructor           search instructor name.
     *                                   Rule: firstName + lastName begins with searchInstructor
     *                                   or firstName + ' ' + lastName begins with searchInstructor
     *                                   or firstName begins with searchInstructor
     *                                   or lastName begins with searchInstructor.
     * @param searchDayOfWeek            search day of week. Matches *any* class in the section in the search day of week.
     * @param searchClassTime            search class time. Matches *any* class in the section contains the search class time.
     * @param searchClassLocations       search class locations.
     *                                   Matches *any* class in the section contains *any* location from the search class locations.
     * @param searchCourseType           search course type. See {@link cn.edu.sustech.cs307.service.StudentService.CourseType}
     * @param ignoreFull                 whether or not to ignore full course sections.
     * @param ignoreConflict             whether or not to ignore course or time conflicting course sections.
     *                                   Note that a section is both course and time conflicting with itself.
     *                                   See {@link cn.edu.sustech.cs307.dto.CourseSearchEntry#conflictCourseNames}
     * @param ignorePassed               whether or not to ignore the student's passed courses.
     * @param ignoreMissingPrerequisites whether or not to ignore courses with missing prerequisites.
     * @param pageSize                   the page size, effectively `limit pageSize`.
     *                                   It is the number of {@link cn.edu.sustech.cs307.dto.CourseSearchEntry}
     * @param pageIndex                  the page index, effectively `offset pageIndex * pageSize`.
     *                                   If the page index is so large that there is no message,return an empty list
     * @return a list of search entries. See {@link cn.edu.sustech.cs307.dto.CourseSearchEntry}
     */
    List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid,
                                         @Nullable String searchName, @Nullable String searchInstructor,
                                         @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime,
                                         @Nullable List<String> searchClassLocations,
                                         CourseType searchCourseType,
                                         boolean ignoreFull, boolean ignoreConflict,
                                         boolean ignorePassed, boolean ignoreMissingPrerequisites,
                                         int pageSize, int pageIndex);

    /**
     * It is the course selection function according to the studentId and courseId.
     * The test case can be invalid data or conflict info, so that it can return 8 different
     * types of enroll results.
     *
     * It is possible for a student-course have ALREADY_SELECTED and ALREADY_PASSED or PREREQUISITES_NOT_FULFILLED.
     * Please make sure the return priority is the same as above in similar cases.
     * {@link cn.edu.sustech.cs307.service.StudentService.EnrollResult}
     *
     * To check whether prerequisite courses are available for current one, only check the
     * grade of prerequisite courses are >= 60 or PASS
     *
     * @param studentId
     * @param sectionId the id of CourseSection
     * @return See {@link cn.edu.sustech.cs307.service.StudentService.EnrollResult}
     */
    EnrollResult enrollCourse(int studentId, int sectionId);

    /**
     * Drop a course section for a student
     *
     * @param studentId
     * @param sectionId
     * @throws IllegalStateException if the student already has a grade for the course section.
     */
    void dropCourse(int studentId, int sectionId) throws IllegalStateException;

    /**
     * It is used for importing existing data from other sources.
     * <p>
     * With this interface, staff for teaching affairs can bypass the
     * prerequisite fulfillment check to directly enroll a student in a course
     * and assign him/her a grade.
     *
     * If the scoring scheme of a course is one type in pass-or-fail and hundredmark grade,
     * your system should not accept the other type of grade.
     *
     * Course section's left capacity should remain unchanged after this method.
     *
     * @param studentId
     * @param sectionId We will get the sectionId of one section first
     *                  and then invoke the method by using the sectionId.
     * @param grade     Can be null
     */
    void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade);

    /**
     * Return a course table in current week according to the date.
     *
     * @param studentId
     * @param date
     * @return the student's course table for the entire week of the date.
     * Regardless which day of week the date is, return Monday-to-Sunday course table for that week.
     */
    CourseTable getCourseTable(int studentId, Date date);

}
