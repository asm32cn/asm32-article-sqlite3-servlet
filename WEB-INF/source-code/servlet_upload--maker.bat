@echo off

rem set cmdLine=javac -cp lib\servlet-api.jar servlet_upload.java -d ..\classes -Xlint:deprecation
set cmdLine=javac -cp lib\servlet-api.jar servlet_upload.java -d ..\classes

echo %cmdLine%

%cmdLine%

pause
