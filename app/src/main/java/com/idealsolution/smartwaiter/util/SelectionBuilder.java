/*
 * Copyright 2014 Google Inc. All rights reserved.
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
 * Modifications:
 * -Imported from AOSP frameworks/base/core/java/com/android/internal/content
 * -Changed package name
 */

package com.idealsolution.smartwaiter.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.idealsolution.smartwaiter.contract.SmartWaiterContract;
import com.idealsolution.smartwaiter.database.SmartWaiterDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


import static com.idealsolution.smartwaiter.util.LogUtils.LOGV;
import static com.idealsolution.smartwaiter.util.LogUtils.makeLogTag;

/**
 * Helper for building selection clauses for {@link SQLiteDatabase}. Each
 * appended clause is combined using {@code AND}. This class is <em>not</em>
 * thread safe.
 */
public class SelectionBuilder {
    private static final String TAG = makeLogTag(SelectionBuilder.class);

    private String mTable = null;
    private Map<String, String> mProjectionMap = Maps.newHashMap();
    private StringBuilder mSelection = new StringBuilder();
    private ArrayList<String> mSelectionArgs = Lists.newArrayList();
    private String mGroupBy = null;
    private String mHaving = null;

    /**
     * Reset any internal state, allowing this builder to be recycled.
     */
    public SelectionBuilder reset() {
        mTable = null;
        mGroupBy = null;
        mHaving = null;
        mSelection.setLength(0);
        mSelectionArgs.clear();
        return this;
    }

    /**
     * Append the given selection clause to the internal state. Each clause is
     * surrounded with parenthesis and combined using {@code AND}.
     */
    public SelectionBuilder where(String selection, String... selectionArgs) {
        if (TextUtils.isEmpty(selection)) {
            if (selectionArgs != null && selectionArgs.length > 0) {
                throw new IllegalArgumentException(
                        "Valid selection required when including arguments=");
            }

            // Shortcut when clause is empty
            return this;
        }

        if (mSelection.length() > 0) {
            mSelection.append(" AND ");
        }

        mSelection.append("(").append(selection).append(")");
        if (selectionArgs != null) {
            Collections.addAll(mSelectionArgs, selectionArgs);
        }

        return this;
    }

    public SelectionBuilder groupBy(String groupBy) {
        mGroupBy = groupBy;
        return this;
    }

    public SelectionBuilder having(String having) {
        mHaving = having;
        return this;
    }

    public SelectionBuilder table(String table) {
        mTable = table;
        return this;
    }

    /**
     * Replace positional params in table. Use for JOIN ON conditions.
     */
    public SelectionBuilder table(String table, String... tableParams) {
        if (tableParams != null && tableParams.length > 0) {
            String[] parts = table.split("[?]", tableParams.length + 1);
            StringBuilder sb = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                sb.append('"').append(tableParams[i - 1]).append('"')
                        .append(parts[i]);
            }
            mTable = sb.toString();
        } else {
            mTable = table;
        }
        return this;
    }

    private void assertTable() {
        if (mTable == null) {
            throw new IllegalStateException("Table not specified");
        }
    }

    public SelectionBuilder mapToTable(String column, String table) {
        mProjectionMap.put(column, table + "." + column);
        return this;
    }

    public SelectionBuilder map(String fromColumn, String toClause) {
        mProjectionMap.put(fromColumn, toClause + " AS " + fromColumn);
        return this;
    }

    /**
     * Return selection string for current internal state.
     *
     * @see #getSelectionArgs()
     */
    public String getSelection() {
        return mSelection.toString();
    }

    /**
     * Return selection arguments for current internal state.
     *
     * @see #getSelection()
     */
    public String[] getSelectionArgs() {
        return mSelectionArgs.toArray(new String[mSelectionArgs.size()]);
    }

    private void mapColumns(String[] columns) {
        boolean contains_distinct = false; //01/04/2015
        for (int i = 0; i < columns.length; i++) {
            //Inicio 01/04/2015
            //final String target = mProjectionMap.get(columns[i]);
            contains_distinct = columns[i].contains(SmartWaiterContract.QUERY_DISTINCT);
            final String colName = (contains_distinct ? columns[i].replace(SmartWaiterContract.QUERY_DISTINCT, "") : columns[i]);
            final String target = mProjectionMap.get(colName);
            //Fin 01/04/2015
            if (target != null) {
                //Inicio 01/04/2015
                //columns[i] =target;
                columns[i] = (contains_distinct ? SmartWaiterContract.QUERY_DISTINCT + target : target);
                //Fin 01/04/2015
            }
        }
    }

    @Override
    public String toString() {
        return "SelectionBuilder[table=" + mTable + ", selection=" + getSelection()
                + ", selectionArgs=" + Arrays.toString(getSelectionArgs())
                + "projectionMap = " + mProjectionMap + " ]";
    }

    /**
     * Execute query using the current internal state as {@code WHERE} clause.
     */
    public Cursor query(SmartWaiterDatabase db, String[] columns, String orderBy) {
        return query(db, columns, orderBy, null);
    }

    /**
     * Execute query using the current internal state as {@code WHERE} clause.
     */
    public Cursor query(SmartWaiterDatabase db, String[] columns, String orderBy,
                        String limit) {
        assertTable();
        if (columns != null) mapColumns(columns);
        LOGV(TAG, "query(columns=" + Arrays.toString(columns)
                + ") " + this);
        return db.query(mTable, columns, getSelection(), getSelectionArgs(), mGroupBy,
                mHaving, orderBy, limit); //28/05/2015
    }

//    /**
//     * Execute update using the current internal state as {@code WHERE} clause.
//     */
//    public int update(SQLiteDatabase db, ContentValues values) {
//        assertTable();
//        LOGV(TAG, "update() " + this);
//        return db.update(mTable, values, getSelection(), getSelectionArgs());
//    }
//
//    /**
//     * Execute delete using the current internal state as {@code WHERE} clause.
//     */
//    public int delete(SQLiteDatabase db) {
//        assertTable();
//        LOGV(TAG, "delete() " + this);
//        return db.delete(mTable, getSelection(), getSelectionArgs());
//    }
}
