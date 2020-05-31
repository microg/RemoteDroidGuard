all:
	$(TOOL_CHAIN)gcc -o libdghelpernative.so -shared -llog aihl.c nativehook.c

install:
	adb push libdghelpernative.so /data/data/org.microg.gms.droidguard/lib/libdghelpernative.so

