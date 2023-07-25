select count(*) from commits;

select AUTHOR, count(*) from COMMITS group by AUTHOR order by 2 desc;

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

SELECT EXTRACT(YEAR FROM epoch) as "Year"
     , COUNT(DISTINCT AUTHOR) AS  "Authors"
     , COUNT(*) AS "Commits"
     , COUNT(*) / COUNT(DISTINCT AUTHOR) AS "CommitsPerAuthor"
     , COUNT(DISTINCT PROJ) AS "Projects"
     , COUNT(*) / COUNT(DISTINCT PROJ) AS "CommitsPerProject"
FROM COMMITS
GROUP BY EXTRACT(YEAR FROM epoch)
ORDER BY 1
;

SELECT COUNT(*) FROM FILES;

SELECT * FROM files;

SELECT FILENAME
     , COUNT(*)
  FROM FILES
GROUP BY FILENAME
HAVING COUNT(*) > 1
ORDER BY 2 DESC;

SELECT type, count(*) from files group by type order by 2 desc;

SELECT count(*) FROM diffs;

SELECT proj, commit, count(*)
 FROM diffs
GROUP BY proj, commit
ORDER BY 3 DESC;

select count(*) from commits where proj = 'eqa-common-security2';
select count(distinct commit) from diffs;

select proj, NEWPATH, count(*) from DIFFS
where CHANGETYPE = 'MODIFY'
  and filetype = 'java'
group by proj, NEWPATH
having count(*) > 1
order by 1, 3 desc;