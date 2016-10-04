#include <stdint.h>

struct dvm_functions
{
  void* (*dvmFindLoadedClass)(const char*);
  void* (*dvmFindVirtualMethodHierByDescriptor)(void*, const char*, const char*);
  void* (*dvmFindDirectMethodByDescriptor)(void*, const char*, const char*);
};

struct method {
  void *clazz;
  uint32_t a; // accessflags

  uint16_t methodIndex;

  uint16_t registersSize;  /* ins + locals */
  uint16_t outsSize;
  uint16_t insSize;

  /* method name, e.g. "<init>" or "eatLunch" */
  const char* name;

  /*
   * Method prototype descriptor string (return and argument types).
   *
   * TODO: This currently must specify the DexFile as well as the proto_ids
   * index, because generated Proxy classes don't have a DexFile.  We can
   * remove the DexFile* and reduce the size of this struct if we generate
   * a DEX for proxies.
   */
  struct DexProto {
    uint32_t* dexFile;     /* file the idx refers to */
    uint32_t protoIdx;                /* index into proto_ids table of dexFile */
  } prototype;

  /* short-form method descriptor string */
  const char* shorty;

  /*
   * The remaining items are not used for abstract or native methods.
   * (JNI is currently hijacking "insns" as a function pointer, set
   * after the first call.  For internal-native this stays null.)
   */

  /* the actual code */
  uint16_t* insns;
};

void dalvik_hook_resolv_dvm();
void dalvik_hook_add(char *cls_name, char *mthd_name, char *sig_str, char *cls_name_hk, char *mthd_name_hk, char *sig_str_hk);

