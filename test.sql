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
         inner join commits b on a.COMMIT = b.commit_id
where COMMIT = 'd76554b6aa6d2cba1121d0340210916c126fe426'
;

select distinct edittype
from DIFFSEDITS;

select proj, epoch, commit_id, delete, insert, replace from (
select a.proj,
       b.EPOCH, b.commit_id,
       SUM(CASE WHEN a.edittype = 'DELETE' THEN a.lines ELSE 0 END)  as DELETE,
       SUM(CASE WHEN a.edittype = 'INSERT' THEN a.lines ELSE 0 END)  as INSERT,
       SUM(CASE WHEN a.edittype = 'REPLACE' THEN a.lines ELSE 0 END) as REPLACE
from DIFFSEDITS a
         INNER JOIN commits b ON a.commit = b.commit_id
        INNER JOIN DIFFENTRIES c ON a.commit = c.COMMIT1
where c.filetype = 'java'
group by a.proj, b.EPOCH, b.commit_id) a
-- where delete is null and insert > 0 and replace is null
order by epoch DESC
;

select * from DIFFSEDITS a inner join commits b on a.commit = b.commit_id;

select *
from DIFFENTRIES
where COMMIT1 = '762cf8b3ce01d6a10d23da4e4d2111b208a6aae1';

select *
from DIFFSEDITS a INNER JOIN commits b ON a.commit = b.commit_id
where COMMIT = '762cf8b3ce01d6a10d23da4e4d2111b208a6aae1'
;

select *
from diffs
where COMMIT = '2797418dd389ba1dee39aa3c23e7997bb98a06cd'
;