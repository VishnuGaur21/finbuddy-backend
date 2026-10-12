@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;c:\finbuddy1\tools\apache-maven-3.9.9\bin;%PATH%"
call "c:\finbuddy1\tools\apache-maven-3.9.9\bin\mvn.cmd" %*
