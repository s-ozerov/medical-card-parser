@@echo on

FOR %%F IN (.\*.jar) DO (
 set filename=%%F
 goto run
)

:run
start jre17windows\bin\javaw.exe -jar "%filename%"