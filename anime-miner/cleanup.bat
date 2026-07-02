@echo off
echo ==========================================
echo Anime Miner Cleanup Utility
echo ==========================================
echo This will delete the node_modules folder,
echo package-lock.json, and package.json to free up space.
echo.
pause

echo Deleting node_modules...
rmdir /s /q node_modules

echo Deleting package-lock.json...
del /q package-lock.json

echo Deleting package.json...
del /q package.json

echo.
echo Cleanup complete! You can safely delete this folder now.
pause
