@echo off

set strCmd=javac -cp lib/servlet-api.jar -d ..\classes asm32_article_web_servlet.java
echo #%strCmd%
%strcmd%

pause
