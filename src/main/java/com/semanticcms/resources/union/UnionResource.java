/*
 * semanticcms-resources-union - Combines multiple sets of SemanticCMS resources.
 * Copyright (C) 2017, 2020, 2021, 2022, 2024  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-resources-union.
 *
 * semanticcms-resources-union is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-resources-union is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-resources-union.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.resources.union;

import com.aoapps.net.Path;
import com.semanticcms.core.resources.Resource;
import com.semanticcms.core.resources.ResourceConnection;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Combines multiple {@link Resource resources} into a single resource.
 *
 * <p>TODO: Future versions could be more complicated than this current sequential scan.
 * A map could be kept of which underlying stores each path is found in, and requests
 * could be round-robin distributed between then when the resource is found in multiple
 * underlying stores.  There are probably other complexities that could be added, but
 * this super simple implementation suffices, even though it relies on a high performance
 * implementation of the underlying implementations {@link Resource#exists()}.</p>
 *
 * <p>TODO: Future versions could also automatically switch to the next copy when an error occurs.
 * This could help mask underlying I/O issues.  But perhaps this is best handled by a resource store
 * dedicated to this purpose?</p>
 */
public class UnionResource extends Resource {

  private final Resource[] resources;

  /**
   * The last index where the resource was found existing.
   * Searches start from this index.
   */
  private volatile int lastExistsIndex;

  /**
   * Creates a new combined resource.
   *
   * @param store  A defensive copy is made
   */
  public UnionResource(UnionResourceStore store, Path path, Resource[] resources) {
    super(store, path);
    if (resources.length == 0) {
      throw new IllegalArgumentException("At least one resource required");
    }
    this.resources = Arrays.copyOf(resources, resources.length);
  }

  @Override
  public UnionResourceStore getStore() {
    return (UnionResourceStore) store;
  }

  /**
   * Returns {@link #isFilePreferred()} from a resource that {@link #exists()}.
   * If no resource exists, returns {@link #isFilePreferred()} from the last resource where did exist,
   * or the first resource if never found.
   *
   * <p>TODO: This should be made consistent with implementation of {@link #getFile()} below.</p>
   */
  @Override
  public boolean isFilePreferred() throws IOException {
    final int startIndex = lastExistsIndex;
    int i = startIndex;
    do {
      Resource resource = resources[i];
      if (resource.exists()) {
        if (i != startIndex) {
          lastExistsIndex = startIndex;
        }
        return resource.isFilePreferred();
      }
    } while ((i = (i + 1) % resources.length) != startIndex);
    return resources[startIndex].isFilePreferred();
  }

  /**
   * Returns {@link #getFile()} from a resource that {@link #exists()} and returns non-null from {@link #getFile()}.
   * If doesn't exist on any resource, returns the first non-null result of {@link #getFile()} in
   * order of {@link #resources}.
   * Finally returns {@code null} if no non-null results found.
   */
  @Override
  public File getFile() throws IOException {
    final int startIndex = lastExistsIndex;
    boolean hasExists = false;
    File lowestIndexFile = null;
    int lowestIndex = 0;
    int i = startIndex;
    do {
      Resource resource = resources[i];
      boolean exists = resource.exists();
      if (exists) {
        hasExists = true;
      }
      File file = resource.getFile();
      if (file != null) {
        if (exists) {
          if (i != startIndex) {
            lastExistsIndex = startIndex;
          }
          return file;
        }
        if (lowestIndexFile == null || i < lowestIndex) {
          lowestIndexFile = file;
          lowestIndex = i;
        }
      }
    } while ((i = (i + 1) % resources.length) != startIndex);
    return hasExists ? null : lowestIndexFile;
  }

  /**
   * Returns {@link #open()} from a resource that {@link #exists()}.
   * If no resource exists, returns {@link #open()} from the last resource where did exist,
   * or the first resource if never found.
   *
   * <p>TODO: Have an affinity for local-file resources like done in {@link #getFile()}?</p>
   */
  @Override
  public UnionResourceConnection open() throws IOException {
    final int startIndex = lastExistsIndex;
    // The first connection opened will be held open as a fall-back if none exist.
    ResourceConnection firstConn = null;
    try {
      int i = startIndex;
      do {
        boolean isFirstConn = i == startIndex;
        ResourceConnection conn = resources[i].open();
        try {
          if (isFirstConn) {
            assert firstConn == null;
            firstConn = conn;
          } else {
            assert firstConn != null;
          }
          if (conn.exists()) {
            if (i != startIndex) {
              lastExistsIndex = startIndex;
            }
            ResourceConnection returnMe = conn;
            // Make sure not closed in finally blocks
            conn = null;
            if (isFirstConn) {
              firstConn = null;
            }
            // Wrap and return
            return new UnionResourceConnection(this, returnMe);
          }
        } finally {
          // Close any connection that is not being held open and has not been returned
          if (!isFirstConn && conn != null) {
            conn.close();
          }
        }
      } while ((i = (i + 1) % resources.length) != startIndex);
      ResourceConnection returnMe = firstConn;
      assert returnMe != null;
      // Make sure not closed in finally block
      firstConn = null;
      // Wrap and return
      return new UnionResourceConnection(this, returnMe);
    } finally {
      // Close any connection held open and not returned
      if (firstConn != null) {
        firstConn.close();
      }
    }
  }
}
