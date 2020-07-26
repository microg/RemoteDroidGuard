#!/bin/sh
# SPDX-FileCopyrightText: 2016, microG Project Team
# SPDX-License-Identifier: Apache-2.0

SCRIPT=`readlink -f "$0"`
SCRIPTPATH=`dirname "$SCRIPT"`
DG=`readlink -f "$1"`

D2J_NAMES="d2j-dex2jar.sh d2j-dex2jar dex2jar.sh dex2jar"
for i in $D2J_NAMES; do
  d2j=`which d2j-dex2jar.sh`
  if [ "$d2j" = "" ]; then break; fi
done
if [ "$d2j" = "" ]; then
  echo "dex2jar not found, please make sure dex2jar is in \$PATH with one of these names: $D2J_NAMES"
  exit 1
fi
echo "Found dex2jar: $d2j"

javac=`which javac`
if [ "$javac" = "" ]; then
  echo "javac not found, please make sure javac is in \$PATH"
  exit 1
fi
echo "Found javac: $javac"

java=`which java`
if [ "$java" = "" ]; then
  echo "java not found, please make sure java is in \$PATH"
  exit 1
fi
echo "Found java: $java"

faketime=`which faketime`
if [ "$faketime" = "" ]; then
  echo "faketime not found, please make sure faketime is in \$PATH"
  exit 1
fi
echo "Found faketime: $faketime"

TMPDIR=`mktemp -d`
cd $SCRIPTPATH
echo "Compiling decryptor"
$javac decrypt.java
echo "Decrypting $DG to $TMPDIR/droidguasso.apk"
$java decrypt "$DG" "$TMPDIR/droidguasso.apk"
$faketime -f '1970-01-01 01:00:00' $d2j -f -o "../app/libs/droidguasso.jar" "$TMPDIR/droidguasso.apk"
rm -rf "$TMPDIR"
