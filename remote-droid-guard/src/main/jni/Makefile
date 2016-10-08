all:
	$(TOOL_CHAIN)gcc -o libdalvikhook.so -shared -llog dalvik_hook.c

install:
	adb push libdalvikhook.so /data/data/org.microg.gms.droidguard/lib/libdalvikhook.so

