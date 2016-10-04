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
  method_hook->clazz = buf->clazz;     //write backup to hook method
  method_hook->insns = buf->insns;
  free(buf);
}

