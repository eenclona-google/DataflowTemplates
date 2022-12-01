/*
 * Copyright (C) 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.teleport.v2.templates.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Session object to store mapping information as per the session file generated by HarbourBridge.
 */
public class Session implements Serializable {
  /** Maps the HarbourBridge table ID to the Spanner schema details. */
  private final Map<String, CreateTable> spSchema;

  /** Maps the Spanner table ID to the synthetic PK. */
  private final Map<String, SyntheticPKey> syntheticPKeys;

  /** Maps the HarbourBridge table ID to the Source schema details. */
  private final Map<String, SrcSchema> srcSchema;

  /** Maps the source table name to the Spanner table name and column details. */
  private Map<String, NameAndCols> toSpanner;

  /** Maps the source table to the HarbourBridge internal ID name. */
  private Map<String, NameAndCols> srcToID;

  /** Denotes whether the session file is empty or not. */
  private boolean empty;

  public Session() {
    this.spSchema = new HashMap<String, CreateTable>();
    this.syntheticPKeys = new HashMap<String, SyntheticPKey>();
    this.srcSchema = new HashMap<String, SrcSchema>();
    this.toSpanner = new HashMap<String, NameAndCols>();
    this.srcToID = new HashMap<String, NameAndCols>();
    this.empty = true;
  }

  public Session(
      Map<String, CreateTable> spSchema,
      Map<String, SyntheticPKey> syntheticPKeys,
      Map<String, SrcSchema> srcSchema) {
    this.spSchema = (spSchema == null) ? (new HashMap<String, CreateTable>()) : spSchema;
    this.syntheticPKeys =
        (syntheticPKeys == null) ? (new HashMap<String, SyntheticPKey>()) : syntheticPKeys;
    this.srcSchema = (srcSchema == null) ? (new HashMap<String, SrcSchema>()) : srcSchema;
    this.empty = (spSchema == null || srcSchema == null);
  }

  public Map<String, CreateTable> getSpSchema() {
    return spSchema;
  }

  public Map<String, SyntheticPKey> getSyntheticPks() {
    return syntheticPKeys;
  }

  public Map<String, SrcSchema> getSrcSchema() {
    return srcSchema;
  }

  public Map<String, NameAndCols> getToSpanner() {
    return toSpanner;
  }

  public void setToSpanner(Map<String, NameAndCols> toSpanner) {
    this.toSpanner = toSpanner;
  }

  public Map<String, NameAndCols> getSrcToID() {
    return srcToID;
  }

  public void setSrcToID(Map<String, NameAndCols> srcToID) {
    this.srcToID = srcToID;
  }

  public void computeToSpanner() {
    // We iterate over spSchema because srcSchema might have extra columns that were dropped.
    for (String tableId : spSchema.keySet()) {
      CreateTable spTable = spSchema.get(tableId);
      SrcSchema srcTable = srcSchema.get(tableId);
      Map<String, String> cols = new HashMap<String, String>();
      // We iterate over spTable columns because the source might have extra columns that were
      // dropped.
      for (String colId : spTable.getColIds()) {
        // We add this check because spanner can have extra columns for synthetic PK.
        if (srcTable.getColDefs().containsKey(colId)) {
          cols.put(
              srcTable.getColDefs().get(colId).getName(),
              spTable.getColDefs().get(colId).getName());
        }
      }
      NameAndCols nameAndCols = new NameAndCols(spTable.getName(), cols);
      this.toSpanner.put(srcTable.getName(), nameAndCols);
    }
  }

  public void computeSrcToID() {
    for (String tableId : srcSchema.keySet()) {
      SrcSchema srcTable = srcSchema.get(tableId);
      Map<String, String> cols = new HashMap<String, String>();
      for (String colId : srcTable.getColIds()) {
        cols.put(srcTable.getColDefs().get(colId).getName(), colId);
      }
      NameAndCols nameAndCols = new NameAndCols(tableId, cols);
      this.srcToID.put(srcTable.getName(), nameAndCols);
    }
  }

  public boolean isEmpty() {
    return empty;
  }

  public void setEmpty(boolean empty) {
    this.empty = empty;
  }

  public String toString() {
    return String.format(
        "{ 'spSchema': '%s', 'syntheticPKeys': '%s', 'srcSchema': '%s', 'toSpanner':"
            + " '%s','srcToID': '%s', 'empty': '%s' }",
        spSchema, syntheticPKeys, srcSchema, toSpanner, srcToID, empty);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Session)) {
      return false;
    }
    final Session other = (Session) o;
    return this.empty == other.empty
        && this.spSchema.equals(other.spSchema)
        && this.syntheticPKeys.equals(other.syntheticPKeys)
        && this.srcSchema.equals(other.srcSchema);
  }
}
