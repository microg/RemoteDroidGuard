# Copyright (c) 2015 μg Project Team
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := RemoteDroidGuard
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := remote-droid-guard

LOCAL_PRIVILEGED_MODULE := true

droidguard_root  := $(LOCAL_PATH)
droidguard_dir   := remote-droid-guard/
droidguard_out   := $(OUT_DIR)/target/common/obj/APPS/$(LOCAL_MODULE)_intermediates
droidguard_build := $(droidguard_root)/build
droidguard_apk   := $(droidguard_dir)/build/outputs/apk/remote-droid-guard-release-unsigned.apk

$(droidguard_root)/$(droidguard_apk):
	rm -Rf $(droidguard_build)
	mkdir -p $(droidguard_out)
	ln -s $(droidguard_out) $(droidguard_build)
	echo "sdk.dir=$(ANDROID_HOME)" > $(droidguard_root)/local.properties
	cd $(droidguard_root) && JAVA_TOOL_OPTIONS="$(JAVA_TOOL_OPTIONS) -Dfile.encoding=UTF8" ./gradlew assembleRelease

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(droidguard_apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)
