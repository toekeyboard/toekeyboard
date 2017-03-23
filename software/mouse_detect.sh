ls -1 /dev/input/event* > mouse_connected
ls -1 /dev/input/event* > mouse_disconnected

diff mouse_connectded mouse_disconnected
