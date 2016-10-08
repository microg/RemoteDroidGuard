/*
Library to hook dalvik methods in runtime.
The idea came from https://github.com/crmulliner/ddi
*/

#include <dlfcn.h>
#include <android/log.h>
#include <stdlib.h>

#include "dalvik_hook.h"

#define  LOG_TAG    "dalvik_hook"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static struct dvm_functions d;

void dalvik_hook_resolv_dvm()
{
  void *handle = dlopen("libdvm.so", RTLD_NOW);
  if(!handle)
    LOGE("error loading libdvm.so\n");

  d.dvmFindLoadedClass = dlsym(handle, "_Z18dvmFindLoadedClassPKc");
  if (!d.dvmFindLoadedClass)
    d.dvmFindLoadedClass = dlsym(handle, "dvmFindLoadedClass");

  d.dvmFindVirtualMethodHierByDescriptor = dlsym(handle, "_Z36dvmFindVirtualMethodHierByDescriptorPK11ClassObjectPKcS3_");
  if (!d.dvmFindVirtualMethodHierByDescriptor)
    d.dvmFindVirtualMethodHierByDescriptor = dlsym(handle, "dvmFindVirtualMethodHierByDescriptor");

  d.dvmFindDirectMethodByDescriptor = dlsym(handle, "_Z31dvmFindDirectMethodByDescriptorPK11ClassObjectPKcS3_");
  if (!d.dvmFindDirectMethodByDescriptor)
    d.dvmFindDirectMethodByDescriptor = dlsym(handle, "dvmFindDirectMethodByDescriptor");
}

void dalvik_hook_add(char *cls_name, char *mthd_name, char *sig_str, char *cls_name_hk, char *mthd_name_hk, char *sig_str_hk)
{
  void *cls_ptr;
  struct method *method;
  struct method *method_hook;
  struct method *buf;
  LOGD("hooking method %s %s %s\n", cls_name, mthd_name, sig_str);
  //get origin method
  cls_ptr = d.dvmFindLoadedClass(cls_name);
  method = d.dvmFindVirtualMethodHierByDescriptor(cls_ptr, mthd_name, sig_str);
  if(!method)
    method = d.dvmFindDirectMethodByDescriptor(cls_ptr, mthd_name, sig_str);
  if(!method)
    LOGD("error finding method %s %s %s\n", cls_name, mthd_name, sig_str);
  //get hook method
  cls_ptr = d.dvmFindLoadedClass(cls_name_hk);
  method_hook = d.dvmFindVirtualMethodHierByDescriptor(cls_ptr, mthd_name_hk, sig_str_hk);
  if(!method_hook)
    method_hook = d.dvmFindDirectMethodByDescriptor(cls_ptr, mthd_name_hk, sig_str_hk);
  if(!method_hook)
    LOGD("error finding method %s %s %s\n", cls_name_hk, mthd_name_hk, sig_str_hk);
  //switch methods
  buf = malloc(sizeof(struct method));
  memcpy(buf, method, sizeof(struct method)); //backup origin method
  method->insns = method_hook->insns;
  method->clazz = method_hook->clazz;
  method->registersSize=method_hook->registersSize;
  method->outsSize=method_hook->outsSize;
  method->insSize=method_hook->insSize;
  method_hook->clazz = buf->clazz;     //write backup to hook method
  method_hook->insns = buf->insns;
  method_hook->registersSize=buf->registersSize;
  method_hook->outsSize=buf->outsSize;
  method_hook->insSize=buf->insSize;
  free(buf);
}

__attribute__((constructor))
void dalvik_hook_init(void)
{
  LOGD("starting dalvik_hook\n");
  dalvik_hook_resolv_dvm();
  dalvik_hook_add("Landroid/telephony/TelephonyManager;", "getSubscriberId", "()Ljava/lang/String;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "TelephonyManager_getSubscriberId", "(Landroid/telephony/TelephonyManager;)Ljava/lang/String;");
  dalvik_hook_add("Landroid/telephony/TelephonyManager;", "getDeviceId", "()Ljava/lang/String;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "TelephonyManager_getDeviceId", "(Landroid/telephony/TelephonyManager;)Ljava/lang/String;");
  dalvik_hook_add("Landroid/net/ConnectivityManager;", "getActiveNetworkInfo", "()Landroid/net/NetworkInfo;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "ConnectivityManager_getActiveNetworkInfo", "(Landroid/net/ConnectivityManager;)Landroid/net/NetworkInfo;");
  dalvik_hook_add("Landroid/content/ContextWrapper;", "getPackageName", "()Ljava/lang/String;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "ContextWrapper_getPackageName", "(Ljava/lang/Object;)Ljava/lang/String;");
  dalvik_hook_add("Landroid/content/ContextWrapper;", "getClassLoader", "()Ljava/lang/ClassLoader;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "ContextWrapper_getClassLoader", "(Ljava/lang/Object;)Ljava/lang/ClassLoader;");
  dalvik_hook_add("Ljava/util/TreeSet;", "iterator", "()Ljava/util/Iterator;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "TreeSet_iterator", "(Ljava/util/TreeSet;)Ljava/util/Iterator;");
  dalvik_hook_add("Ljava/util/regex/Pattern;", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "Pattern_matcher", "(Ljava/util/regex/Pattern;Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;");
  dalvik_hook_add("Landroid/os/SystemProperties;", "getInt", "(Ljava/lang/String;I)I",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "SystemProperties_getInt", "(Ljava/lang/String;I)I");
  dalvik_hook_add("Ljava/util/Arrays;", "asList", "([Ljava/lang/Object;)Ljava/util/List;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "Arrays_asList", "([Ljava/lang/Object;)Ljava/util/List;");
  dalvik_hook_add("Landroid/app/ApplicationPackageManager;", "getSystemSharedLibraryNames", "()[Ljava/lang/String;",
                  "Lorg/microg/gms/droidguard/DalvikHook;", "PackageManager_getSystemSharedLibraryNames", "(Ljava/lang/Object;)[Ljava/lang/String;");
}

