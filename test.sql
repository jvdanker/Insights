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
from commits
where proj = 'eqa-common-security2';
select count(distinct commit)
from DIFFENTRIES;

select proj, NEWPATH, count(*)
from DIFFENTRIES
where CHANGETYPE = 'MODIFY'
  and filetype = 'java'
group by proj, NEWPATH
having count(*) > 1
order by 1, 3 desc;

select b.author, a.*
from DIFFSEDITS a
         inner join commits b on a.COMMIT = b.commitId
;

select distinct edittype
from DIFFSEDITS;

select a.proj,
       a.commit,
       b.EPOCH,
       b.AUTHOR,
       SUM(CASE WHEN a.edittype = 'DELETE' THEN a.lines END)  as DELETE,
       SUM(CASE WHEN a.edittype = 'INSERT' THEN a.lines END)  as INSERT,
       SUM(CASE WHEN a.edittype = 'REPLACE' THEN a.lines END) as REPLACE
from DIFFSEDITS a
         INNER JOIN commits b ON a.commit = b.COMMITID
-- where a.commit = '064e628d8aa172f9a57762d36242baa851da3f12'
group by a.proj, a.commit, b.EPOCH, b.AUTHOR
order by b.epoch DESC
;

select a.proj,
       extract(year from b.EPOCH),
       SUM(CASE WHEN a.edittype = 'DELETE' THEN a.lines END)  as DELETE,
       SUM(CASE WHEN a.edittype = 'INSERT' THEN a.lines END)  as INSERT,
       SUM(CASE WHEN a.edittype = 'REPLACE' THEN a.lines END) as REPLACE
from DIFFSEDITS a
         INNER JOIN commits b ON a.commit = b.COMMITID
group by a.proj, extract(year from b.EPOCH)
order by b.epoch DESC
;

select *
from DIFFSEDITS
;

select diff
from diffs
;