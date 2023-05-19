/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
** Modified to support SQLite extensions by the SQLite developers: 
** sqlite-dev@sqlite.org.
*/

package org.sqlite.database;

import android.util.Log;
import android.util.Pair;

import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteException;

import java.io.File;
import java.util.List;

/**
 * Default class used to define the actions to take when the org.sqlite.database corruption is reported
 * by sqlite.
 * <p>
 * An application can specify an implementation of {@link DatabaseErrorHandler} on the
 * following:
 * <ul>
 *   <li>{@link SQLiteDatabase#openOrCreateDatabase(String,
 *      SQLiteDatabase.CursorFactory, DatabaseErrorHandler)}</li>
 *   <li>{@link SQLiteDatabase#openDatabase(String,
 *      SQLiteDatabase.CursorFactory, int, DatabaseErrorHandler)}</li>
 * </ul>
 * The specified {@link DatabaseErrorHandler} is used to handle org.sqlite.database corruption errors, if they
 * occur.
 * <p>
 * If null is specified for DatabaeErrorHandler param in the above calls, then this class is used
 * as the default {@link DatabaseErrorHandler}.
 */
public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {

    private static final String TAG = "DefaultDatabaseErrorHandler";

    /**
     * defines the default method to be invoked when org.sqlite.database corruption is detected.
     * @param dbObj the {@link SQLiteDatabase} object representing the org.sqlite.database on which corruption
     * is detected.
     */
    public void onCorruption(SQLiteDatabase dbObj) {
        Log.e(TAG, "Corruption reported by sqlite on org.sqlite.database: " + dbObj.getPath());

	// If this is a SEE build, do not delete any org.sqlite.database files.
	//
	if( SQLiteDatabase.hasCodec() ) return;

        // is the corruption detected even before org.sqlite.database could be 'opened'?
        if (!dbObj.isOpen()) {
            // org.sqlite.database files are not even openable. delete this org.sqlite.database file.
            // NOTE if the org.sqlite.database has attached databases, then any of them could be corrupt.
            // and not deleting all of them could cause corrupted org.sqlite.database file to remain and
            // make the application crash on org.sqlite.database open operation. To avoid this problem,
            // the application should provide its own {@link DatabaseErrorHandler} impl class
            // to delete ALL files of the org.sqlite.database (including the attached databases).
            deleteDatabaseFile(dbObj.getPath());
            return;
        }

        List<Pair<String, String>> attachedDbs = null;
        try {
            // Close the org.sqlite.database, which will cause subsequent operations to fail.
            // before that, get the attached org.sqlite.database list first.
            try {
                attachedDbs = dbObj.getAttachedDbs();
            } catch (SQLiteException e) {
                /* ignore */
            }
            try {
                dbObj.close();
            } catch (SQLiteException e) {
                /* ignore */
            }
        } finally {
            // Delete all files of this corrupt org.sqlite.database and/or attached databases
            if (attachedDbs != null) {
                for (Pair<String, String> p : attachedDbs) {
                    deleteDatabaseFile(p.second);
                }
            } else {
                // attachedDbs = null is possible when the org.sqlite.database is so corrupt that even
                // "PRAGMA database_list;" also fails. delete the main org.sqlite.database file
                deleteDatabaseFile(dbObj.getPath());
            }
        }
    }

    private void deleteDatabaseFile(String fileName) {
        if (fileName.equalsIgnoreCase(":memory:") || fileName.trim().length() == 0) {
            return;
        }
        Log.e(TAG, "deleting the org.sqlite.database file: " + fileName);
        try {
            SQLiteDatabase.deleteDatabase(new File(fileName));
        } catch (Exception e) {
            /* print warning and ignore exception */
            Log.w(TAG, "delete failed: " + e.getMessage());
        }
    }
}
