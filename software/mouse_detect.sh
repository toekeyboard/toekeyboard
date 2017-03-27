#http://www.unix.com/shell-programming-and-scripting/116783-bash-replacement-getchar.html

echo "Disconnect mouse. Then press any key to continue."
read -n1 kbd
ls -1 /dev/input/event* > mouse_connection_disconnected

echo "Connect mouse.  Then press any key to continue."
read -n1 kbd
ls -1 /dev/input/event* > mouse_connection_connected

diff mouse_connection_disconnected mouse_connection_connected

rm mouse_connection_*
