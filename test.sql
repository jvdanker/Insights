select count(*)
from commits;

select AUTHOR, count(*)
from COMMITS
group by AUTHOR
order by 2 desc;

select proj, max(epoch), count(*)
from commits
group by proj
order by 3 desc
;

SELECT EXTRACT(YEAR FROM epoch) as "Year"
     , COUNT(*)
FROM COMMITS
GROUP BY EXTRACT(YEAR FROM epoch)
ORDER BY 1
;

SELECT EXTRACT(YEAR FROM epoch)          as "Year"
     , COUNT(DISTINCT AUTHOR)            AS "Authors"
     , COUNT(*)                          AS "Commits"
     , COUNT(*) / COUNT(DISTINCT AUTHOR) AS "CommitsPerAuthor"
     , COUNT(DISTINCT PROJ)              AS "Projects"
     , COUNT(*) / COUNT(DISTINCT PROJ)   AS "CommitsPerProject"
FROM COMMITS
GROUP BY EXTRACT(YEAR FROM epoch)
ORDER BY 1
;

SELECT COUNT(*)
FROM FILES;

SELECT *
FROM files;

SELECT FILENAME
     , COUNT(*)
FROM FILES
GROUP BY FILENAME
HAVING COUNT(*) > 1
ORDER BY 2 DESC;

SELECT type, count(*)
from files
group by type
order by 2 desc;

SELECT count(*)
FROM DIFFENTRIES;

SELECT proj, commit, count(*)
FROM DIFFENTRIES
GROUP BY proj, commit
ORDER BY 3 DESC;

select count(*)
from commits;

select count(distinct commit)
from DIFFENTRIES;

select proj, NEWPATH, count(*)
from DIFFENTRIES
where CHANGETYPE = 'MODIFY'
  and filetype = 'java'
group by proj, NEWPATH
having count(*) > 1
order by 1, 3 desc;

select *
from DIFFENTRIES
where COMMIT1 = 'd76554b6aa6d2cba1121d0340210916c126fe426'
;

select b.author, a.*
from DIFFSEDITS a
         inner join commits b on a.COMMIT = b.COMMIT_ID
;

select distinct edittype
from DIFFSEDITS;

select edittype, count(*) count
from diffsedits
group by edittype
order by 1;

select proj, epoch, COMMIT_ID, delete, insert, replace
from (select a.proj,
             b.EPOCH,
             b.COMMIT_ID,
             SUM(CASE WHEN a.edittype = 'DELETE' THEN a.lines ELSE 0 END)  as DELETE,
             SUM(CASE WHEN a.edittype = 'INSERT' THEN a.lines ELSE 0 END)  as INSERT,
             SUM(CASE WHEN a.edittype = 'REPLACE' THEN a.lines ELSE 0 END) as REPLACE
      from DIFFSEDITS a
               INNER JOIN commits b ON a.commit = b.COMMIT_ID
               INNER JOIN DIFFENTRIES c ON a.commit = c.COMMIT1
-- where c.filetype = 'java'
      group by a.proj, b.EPOCH, b.COMMIT_ID) a
-- where delete is null and insert > 0 and replace is null
order by epoch DESC
;

select epoch, project, avg(size), sum(delete), sum(insert), sum(replace)
from (select epoch,
             project,
             path,
             filename,
             max(size)                                                     as size,
             SUM(CASE WHEN a.edittype = 'DELETE' THEN a.lines ELSE 0 END)  as DELETE,
             SUM(CASE WHEN a.edittype = 'INSERT' THEN a.lines ELSE 0 END)  as INSERT,
             SUM(CASE WHEN a.edittype = 'REPLACE' THEN a.lines ELSE 0 END) as REPLACE
      from (select c.EPOCH, b.project, b.PATH, b.FILENAME, b.SIZE, d.EDITTYPE, d.LINES
            from DIFFENTRIES a
                     inner join files b on a.NEWPATH = b.FULLPATH and a.PROJ = b.PROJECT
                     inner join commits c on a.COMMIT1 = c.COMMIT_ID
                     inner join DIFFSEDITS d on a.COMMIT1 = d.COMMIT and a.NEWPATH = d.FILENAME
            where a.CHANGETYPE = 'MODIFY') a
      group by epoch, project, path, filename
      order by 1 desc, 2, 3)
group by epoch, project
order by 1 desc
;

select *
from DIFFSEDITS a
         inner join commits b on a.commit = b.COMMIT_ID;

select *
from DIFFENTRIES;

select *
from diffs;

select NEWPATH, max(SIZE)
from DIFFENTRIES
where CHANGETYPE != 'DELETE'
  AND FILETYPE = 'java'
group by NEWPATH
order by 2 desc;

select *
from FILES
where extension = 'java'
  and module != 'test-all'
  and module != 'presentation'
order by size desc;

select module, count(*)
from FILES
group by module
order by 2 desc;

select path, count(*)
from FILES
group by path
order by 1;

select *
from FILES
where module = 'exi';

select object_id, FILENAME, count(*)
from FILES
group by object_id, filename
having count(*) > 1;

select count(distinct FILE_ID)
from methods;
select count(distinct FILEID)
from DIFFSEDITS;


select OBJECT_ID
     , PROJECT
     , MODULE
     , CLASS
     , METHOD
     , COMPLEXITY
     , STATEMENTS
     , SUM(insert)
     , SUM(delete)
     , SUM(replace)
FROM (select OBJECT_ID
           , PROJECT
           , MODULE
           , CLASS
           , METHOD
           , COMPLEXITY
           , STATEMENTS
           , CASE WHEN EDITTYPE = 'INSERT' THEN 1 END  insert
           , CASE WHEN EDITTYPE = 'DELETE' THEN 1 END  delete
           , CASE WHEN EDITTYPE = 'REPLACE' THEN 1 END replace
      FROM (select f.OBJECT_ID
                 , f.PROJECT
                 , f.MODULE
                 , m.CLASS
                 , m.METHOD
                 , m.COMPLEXITY
                 , m.STATEMENTS
                 , e.EDITTYPE
            from files f
                     inner join diffsedits e on f.OBJECT_ID = e.FILEID
                     inner join methods m on e.FILEID = m.FILE_ID
            WHERE CASE
                      WHEN e.EDITTYPE = 'INSERT' THEN m.LINESTART > e.BEGINB and m.LINEEND < e.ENDB
                      WHEN e.EDITTYPE = 'DELETE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
                      WHEN e.EDITTYPE = 'REPLACE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
                      END = true))
GROUP BY OBJECT_ID, PROJECT, MODULE, CLASS, METHOD, COMPLEXITY, STATEMENTS
;

select OBJECT_ID
     , PROJECT
     , MODULE
     , CLASS
     , METHOD
     , COMPLEXITY
     , STATEMENTS
     , COUNT(*)
FROM (select e.COMMIT
           , f.OBJECT_ID
           , f.PROJECT
           , f.MODULE
           , m.CLASS
           , m.METHOD
           , m.COMPLEXITY
           , m.STATEMENTS
           , e.EDITTYPE
      from files f
               inner join diffsedits e on f.OBJECT_ID = e.FILEID
               inner join methods m on e.FILEID = m.FILE_ID
      WHERE CASE
                WHEN e.EDITTYPE = 'INSERT' THEN m.LINESTART > e.BEGINB and m.LINEEND < e.ENDB
                WHEN e.EDITTYPE = 'DELETE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
                WHEN e.EDITTYPE = 'REPLACE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
                END = true)
GROUP BY OBJECT_ID, PROJECT, MODULE, CLASS, METHOD, COMPLEXITY, STATEMENTS
HAVING COUNT(*) > 1
;

select f.OBJECT_ID
     , f.PROJECT
     , f.MODULE
     , m.CLASS
     , m.METHOD
     , m.LINESTART
     , m.LINEEND
     , m.COMPLEXITY
     , m.STATEMENTS
     , e.EDITTYPE
     , e.BEGINA
     , e.ENDA
     , e.BEGINB
     , e.ENDB
from files f
         inner join diffsedits e on f.OBJECT_ID = e.FILEID
         inner join methods m on e.FILEID = m.FILE_ID
WHERE CASE
          WHEN e.EDITTYPE = 'INSERT' THEN m.LINESTART > e.BEGINB and m.LINEEND < e.ENDB
          WHEN e.EDITTYPE = 'DELETE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
          WHEN e.EDITTYPE = 'REPLACE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
          END = true;

select de.proj, f.SIZE
, f.FULLPATH, f.FILENAME
from DIFFENTRIES de
inner join files f on de.NEWID = f.OBJECT_ID;

select c.proj
     , c.epoch
     , f.MODULE
     , m.CLASS
     , m.METHOD
     , m.COMPLEXITY
     , m.STATEMENTS
     , e.EDITTYPE
from files f
         inner join diffsedits e on f.OBJECT_ID = e.FILEID
         inner join methods m on e.FILEID = m.FILE_ID
         inner join commits c on e.COMMIT = c.COMMIT_ID
WHERE CASE
          WHEN e.EDITTYPE = 'INSERT' THEN m.LINESTART > e.BEGINB and m.LINEEND < e.ENDB
          WHEN e.EDITTYPE = 'DELETE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
          WHEN e.EDITTYPE = 'REPLACE' THEN m.LINESTART <= e.BEGINA and m.LINEEND >= e.ENDA
          END = true
   and m.COMPLEXITY > 1
order by 1,2 desc,3,4,5;

