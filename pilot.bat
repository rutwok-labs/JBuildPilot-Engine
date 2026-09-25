@echo off
REM JBuildPilot CLI Wrapper
REM Make sure to build the project first: mvn clean install -DskipTests

set ARGS=%*
if "%~1"=="" set ARGS=

mvn exec:java -pl jbuildpilot-cli -Dexec.mainClass="io.github.rutwoklabs.jbuildpilot.cli.JBuildPilotCli" -Dexec.args="%ARGS%" -q
