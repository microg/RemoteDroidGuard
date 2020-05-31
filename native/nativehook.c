#include <fcntl.h>
#include "aihl.h"

#define EMPTY_DIR_PATH "/data/data/org.microg.gms.droidguard/emptydir"

int hooked_open(const char *pathname, int flags) {
  if(!(strcmp(pathname, "/system/xbin") && strcmp(pathname, "/system/bin")))
  {
    pathname = EMPTY_DIR_PATH;
  }
  //call original open function
  return open(pathname, flags);
}

__attribute__((constructor))
void dalvik_hook_init(void)
{
  mkdir(EMPTY_DIR_PATH, 0755);
  void *handle = aihl_load_library("libc.so");
  aihl_hook_symbol(handle, "open", hooked_open, open);
}

