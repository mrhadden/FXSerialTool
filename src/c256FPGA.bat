@echo off

REM Quartus Binaries                
SET QUARTUS_DIR=%QUARTUS_ROOTDIR%\bin64
SET QUARTUS_BIN=quartus_pgm


REM LOCATION OF sof/pof files
REM SET FPGA_DIR=G:\devprojects\c256feonix\fpga
SET FPGA_DIR=%~dp0
SET FPGA_DIR=%FPGA_DIR:~0,-1%

REM DEFAULT VALUES
SET MODE=S
SET GAVIN_CUR=
SET VICKY_CUR=
SET BEATRIX_CUR=
SET QUIET=
SET NONET=
SET QUARTUS_OPT=

echo MODE0:%MODE%

set argCount=0
for %%x in (%*) do (
   set /A argCount+=1
)

IF %argCount% == 0 GOTO usage

for %%x in (%*) do (
	IF [%%x] == [-g] SET GAVIN_CUR=Y
	IF [%%x] == [-v] SET VICKY_CUR=Y
	IF [%%x] == [-b] SET BEATRIX_CUR=Y
	IF [%%x] == [-q] SET QUIET=Y
	IF [%%x] == [-n] SET NONET=Y
	IF [%%x] == [-s] SET MODE=S
	IF [%%x] == [-p] SET MODE=P
)

echo MODE1:%MODE%

REM FPGA NAMES
REM SET GAVIN_NAME=10M04SCE144
SET GAVIN_NAME=10M04SC
SET VICKY_NAME=10M16SC
SET BEATRIX_NAME=10M04SC

REM FPGA DEVICE IDs
SET GAVIN_DEV=1
SET VICKY_DEV=2
SET BEATRIX_DEV=3

REM SOFT FILE NAMES
SET GAVIN_SOFT=g.sof
SET VICKY_SOFT=v.sof
SET BEATRIX_SOFT=b.sof

REM SOFT PERM NAMES
SET GAVIN_PERM=g.pof
SET VICKY_PERM=v.pof
SET BEATRIX_PERM=b.pof

IF [%QUIET%] == [Y] SET QUARTUS_OPT=--quiet

echo MODE2:%MODE%

IF [%MODE%] == [P] (
GOTO perm_mode
) else (
GOTO soft_mode
)


:soft_mode

IF [%QUIET%] == [] ( 
echo.
echo Mode: Apply Soft
)

SET GAVIN_FILE=%GAVIN_SOFT%
SET VICKY_FILE=%VICKY_SOFT%
SET BEATRIX_FILE=%BEATRIX_SOFT%
GOTO end_mode

:perm_mode

IF [%QUIET%] == [] ( 
echo.
echo Mode: Apply Permanent
)

SET GAVIN_FILE=%GAVIN_PERM%
SET VICKY_FILE=%VICKY_PERM%
SET BEATRIX_FILE=%BEATRIX_PERM%
GOTO end_mode

:end_mode

SET GAVIN_OPT=p;%FPGA_DIR%\%GAVIN_FILE%@%GAVIN_DEV%
SET VICKY_OPT=p;%FPGA_DIR%\%VICKY_FILE%@%VICKY_DEV%
SET BEATRIX_OPT=p;%FPGA_DIR%\%BEATRIX_FILE%@%BEATRIX_DEV%

IF [%GAVIN_CUR%]   == [] SET GAVIN_OPT=s;%GAVIN_NAME%@%GAVIN_DEV%
IF [%VICKY_CUR%]   == [] SET VICKY_OPT=s;%VICKY_NAME%@%VICKY_DEV%
IF [%BEATRIX_CUR%] == [] SET BEATRIX_OPT=s;%BEATRIX_NAME%@%BEATRIX_DEV%

IF [%QUIET%] == [] ( 
echo QUARTUS bin Directory:%QUARTUS_DIR%
echo FPGA Directory:%FPGA_DIR%

echo.
echo Using:
echo 	Gavin  : %GAVIN_OPT%
echo 	Vicky  : %VICKY_OPT%
echo 	Beatrix: %BEATRIX_OPT%

echo.
echo Looking for JTAG device....
)

for /f "tokens=1,*" %%i in ('"%QUARTUS_DIR%\%QUARTUS_BIN% -l"') do ^
if "%%i"=="1)" (
SET BLASTER="%%j"
)

IF [%QUIET%] == [] (
echo Found JTAG Device:%BLASTER%
echo.
)

IF [%NONET%] == [] ( 
echo Programming in 5
timeout 1 > NUL
echo 4
timeout 1 > NUL
echo 3
timeout 1 > NUL
echo 2
timeout 1 > NUL
echo 1
timeout 1 > NUL
)

IF [%QUIET%] == [] ( 
echo Programming....
echo.
REM THIS IS AN EXAMPLE OF A FULL COMMAND LINE
REM G:\devtools\intelFPGA_lite\18.1\quartus\bin64>quartus_pgm --cable="EPT-JTAG-Blaster v1.0 (64) [MBUSB-0]" --mode=jtag -o p;G:\devprojects\c256feonix\fpga\g.sof@1 -o p;G:\devprojects\c256feonix\fpga\v.sof@2 -o s;10M04SC@3
REM echo %QUARTUS_DIR%\%QUARTUS_BIN% %QUARTUS_OPT% --cable="%BLASTER%" --mode=jtag -o %GAVIN_OPT% -o %VICKY_OPT% -o %BEATRIX_OPT%
%QUARTUS_DIR%\%QUARTUS_BIN% %QUARTUS_OPT% --cable=%BLASTER% --mode=jtag -o %GAVIN_OPT% -o %VICKY_OPT% -o %BEATRIX_OPT%
echo.
echo.
echo.
echo.
) else (
%QUARTUS_DIR%\%QUARTUS_BIN% %QUARTUS_OPT% --cable=%BLASTER% --mode=jtag -o %GAVIN_OPT% -o %VICKY_OPT% -o %BEATRIX_OPT% > NULL
)

goto quit

:usage
echo.
echo NOTE: Ensure the debugport USB cable is disconnected and JTAG is connected.
echo.
echo CURRENT ENVIRONMENT:
echo.
echo QUARTUS bin Directory:%QUARTUS_DIR%
echo FPGA Directory:%FPGA_DIR%
echo.
echo.
echo If this doesn't match your configuration change the QUARTUS_DIR and FPGA_DIR variables in this script.
echo QUARTUS_DIR should be set from the Quartus install process.
echo FPGA_DIR defaults to the directory from which it is runs.
echo.
echo.
echo Usage:
echo.
echo Arguments:
echo.
echo -s : soft programming mode, using the .sof file (default mode).
echo.
echo -p : perm programming mode, using the .pof file.
echo.
echo -g : program the Gavin FPGA with the mode appropriate .sof/.pof file.
echo      The file name will be g.sof/.pof
echo.
echo -v : program the Vicky FPGA with the mode appropriate .sof/.pof file.
echo      The file name will be v.sof/.pof
echo.
echo -b : program the Beatrix FPGA with the mode appropriate .sof/.pof file.
echo      The file name will be b.sof/.pof
echo.
echo -n : program without any delay.
echo.
echo -q : quiet mode.
echo.
echo.
echo Example:
echo Soft Programming of Gavin and Vicky
echo Make copies of the current FPGA code as g.sof and v.sof, i.e., CFP9518_GAVIN_-_PatchMay12th.sof to g.sof
echo.
echo Command: c256FPGA.bat -s -g -v
echo.
echo.
echo.
echo.
:quit

