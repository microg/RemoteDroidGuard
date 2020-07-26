/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DroidGuardDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "dg.db";

    public DroidGuardDatabase(Context context) {
        super(context, DB_NAME, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Note: "NON NULL" is actually not a valid sqlite constraint, but this is what we see in the original database *shrug*
        db.execSQL("CREATE TABLE main (a TEXT NOT NULL, b LONG NOT NULL, c LONG NOT NULL, d TEXT NON NULL, e TEXT NON NULL,f BLOB NOT NULL,g BLOB NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS main;");
        onCreate(db);
    }
}
