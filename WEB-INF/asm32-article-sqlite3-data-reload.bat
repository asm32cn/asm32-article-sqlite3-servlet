@echo off

::asm32-article-sqlite3-data-reload.bat

set strCmd=copy E:\PASCAL\asm32.article.sqlite3-20180111\App_Data\asm32.article.sqlite3 .

echo #%strCmd%
%strCmd%

pause
