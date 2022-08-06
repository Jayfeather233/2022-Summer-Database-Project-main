create table userT
(
    id         int primary key,
    type       varchar not null,
    first_name text,
    last_name  text
);

create sequence department_seq;
create table department
(
    id   int primary key default nextval('department_seq'),
    name text not null
);

create sequence major_seq;
create table major
(
    id     int primary key default nextval('major_seq'),
    name   text not null,
    deptID int,
    foreign key (deptID) references department (id)
);

create table student
(
    id           int primary key,
    majorID      int,
    enrolledDate date,
    foreign key (majorID) references major (id),
    foreign key (id) references userT (id)
);

create table course
(
    id        text primary key,
    name      text,
    credit    int,
    classHour int,
    grading   bool
);

create sequence prere_seq;
create table prerequisite
(
    id       int primary key default nextval('prere_seq'),
    courseID text not null,
    count    int,
    foreign key (courseID) references course (id)
);
create sequence cg_seq;
create table courseGroup
(
    id       int primary key default nextval('cg_seq'),
    courseID text not null,
    groupID  int,
    foreign key (courseID) references course (id),
    foreign key (groupID) references prerequisite (id)
);

create table courseMajor
(
    type     bool,
    courseID text primary key,
    majorID  int,
    foreign key (courseID) references course (id),
    foreign key (majorID) references major (id)
);

create sequence semester_seq;
create table semester
(
    id     int primary key default nextval('semester_seq'),
    name   text not null,
    beginT date,
    endT   date
);

create sequence section_seq;
create table section
(
    id           int primary key default nextval('section_seq'),
    courseID     text,
    semesterID   int,
    sectionName  text not null,
    fullCapacity int,
    leftCapacity int,
    foreign key (courseID) references course (id),
    foreign key (semesterID) references semester (id)
);

create table enroll
(
    studentID int,
    sectionID int,
    grade     int,
    foreign key (studentID) references student (id),
    foreign key (sectionID) references section (id),
    primary key (studentID, sectionID)
);

create sequence class_seq;
create table class
(
    id           int primary key default nextval('class_seq'),
    sectionID    int not null,
    instructorID int,
    dayOfWeek    smallint,
    weekList     smallint[],
    classStart   smallint,
    classEnd     smallint,
    location     text,
    foreign key (instructorID) references userT (id),
    foreign key (sectionID) references section (id)
);


create or replace function getWeekNumber(st date, en date) returns integer
as
$$
declare
    re int;
begin
    re = 0;
    while (st <= en)
        loop
            st = st + integer '7';
            re = re + 1;
        end loop;
    return re;
end
$$
    language plpgsql;

create or replace function isPassedPrere(studentid int, courseid text) returns boolean
as
$$
begin
    if not (exists(select * from prerequisite where prerequisite.courseID = $2)) then
        return true;
    else
        return exists(select *
                      from (select p.id as groupId, count(*) over (partition by p.id) as cnt, p.count
                            from course
                                     join prerequisite p on course.id = p.courseID
                                     join courseGroup cG on p.id = cG.groupID
                            where course.id = $2
                              and cG.courseid in
                                  (select section.courseID
                                   from section
                                            join (select e.sectionID
                                                  from enroll e
                                                  where e.studentID = $1
                                                    and grade is not null
                                                    and grade >= 60) ex
                                                 on ex.sectionID = section.id)) qq
                      where cnt = count);
    end if;
end
$$
    language plpgsql;

create or replace function isConflict(stid int, scid int) returns boolean
as
$$
begin
    if exists(with targeted as (select semesterid       as semester,
                                       courseID         as course,
                                       dayOfWeek        as day,
                                       unnest(weeklist) as week,
                                       classStart       as st,
                                       classEnd         as et
                                from class
                                         join section on class.sectionId = section.id
                                where sectionID = $2),
                   enrolled as (select semesterid       as semester,
                                       courseID         as course,
                                       dayOfWeek        as day,
                                       unnest(weeklist) as week,
                                       classStart       as st,
                                       classEnd         as et
                                from (select sectionId, grade from enroll where studentId = $1) as t
                                         join section
                                              on t.sectionId = section.id
                                         join class
                                              on t.sectionId = class.sectionID)
              select
              from enrolled,
                   targeted
              where enrolled.semester = targeted.semester
                and (
                      (enrolled.day = targeted.day
                          and enrolled.week = targeted.week
                          and ((targeted.st between enrolled.st and enrolled.et)
                              or (targeted.et between enrolled.st and enrolled.et)))
                      or (enrolled.course = targeted.course)
                  )) then return true;
    elseif exists(with targeted as (select semesterid       as semester,
                                           courseID         as course
                                    from section
                                    where id = $2),
                       enrolled as (select semesterid       as semester,
                                           courseID         as course
                                    from (select sectionId from enroll where studentId = $1) as t
                                             join section
                                                  on t.sectionId = section.id)
                  select
                  from enrolled,
                       targeted
                  where enrolled.course = targeted.course) then return true;
    else return false;
    end if;
end
$$ language plpgsql;

create or replace function isConflictCourse(scid1 int, scid2 int) returns boolean
as
$$
begin
    if exists(with targeted as (select semesterid       as semester,
                                       courseID         as course,
                                       dayOfWeek        as day,
                                       unnest(weeklist) as week,
                                       classStart       as st,
                                       classEnd         as et
                                from class
                                         join section on class.sectionId = section.id
                                where sectionID = $2),
                   enrolled as (select semesterid       as semester,
                                       courseID         as course,
                                       dayOfWeek        as day,
                                       unnest(weeklist) as week,
                                       classStart       as st,
                                       classEnd         as et
                                from class
                                         join section on class.sectionId = section.id
                                where sectionID = $1)
              select
              from enrolled,
                   targeted
              where enrolled.semester = targeted.semester
                and (
                      (enrolled.day = targeted.day
                          and enrolled.week = targeted.week
                          and ((targeted.st between enrolled.st and enrolled.et)
                              or (targeted.et between enrolled.st and enrolled.et)))
                      or (enrolled.course = targeted.course)
                  )) then return true;
    elseif exists(with targeted as (select semesterid       as semester,
                                           courseID         as course
                                    from section
                                    where id = $2),
                       enrolled as (select semesterid       as semester,
                                           courseID         as course
                                    from section
                                    where id = $1)
                  select
                  from enrolled,
                       targeted
                  where enrolled.course = targeted.course
        ) then return true;
    else return false;
    end if;
end
$$ language plpgsql;
