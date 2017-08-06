/*
 * semanticcms-resources-union - Combines multiple sets of SemanticCMS resources.
 * Copyright (C) 2017  AO Industries, Inc.
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
 * along with semanticcms-resources-union.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.resources.union;

import com.aoindustries.util.AoCollections;
import com.semanticcms.core.resources.Resource;
import com.semanticcms.core.resources.ResourceStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines multiple {@link ResourceStore resource stores} into a single store.
 */
public class UnionResourceStore implements ResourceStore {

	private static final Map<List<ResourceStore>,UnionResourceStore> unionStores = new HashMap<List<ResourceStore>,UnionResourceStore>();

	/**
	 * Gets the union store representing the given set of stores, creating a new store only as-needed.
	 * Only one {@link UnionResourceStore} is created per unique list of underlying stores.
	 *
	 * @param stores  A defensive copy is made
	 */
	public static UnionResourceStore getInstance(ResourceStore ... stores) {
		return getInstance(new ArrayList<ResourceStore>(Arrays.asList(stores)));
	}

	/**
	 * Gets the union store representing the given set of stores, creating a new store only as-needed.
	 * Only one {@link UnionResourceStore} is created per unique list of underlying stores.
	 *
	 * @param stores  Iterated once only.
	 */
	public static UnionResourceStore getInstance(Iterable<ResourceStore> stores) {
		List<ResourceStore> list = new ArrayList<ResourceStore>();
		for(ResourceStore store : stores) list.add(store);
		return getInstance(list);
	}

	/**
	 * Only one {@link UnionResourceStore} is created per unique list of underlying stores.
	 */
	private static UnionResourceStore getInstance(List<ResourceStore> stores) {
		if(stores.isEmpty()) throw new IllegalArgumentException("At least one store required");
		synchronized(unionStores) {
			UnionResourceStore unionStore = unionStores.get(stores);
			if(unionStore == null) {
				unionStore = new UnionResourceStore(stores.toArray(new ResourceStore[stores.size()]));
				unionStores.put(stores, unionStore);
			}
			return unionStore;
		}
	}

	private final ResourceStore[] stores;
	private final List<ResourceStore> unmodifiableStores;

	private UnionResourceStore(ResourceStore[] stores) {
		this.stores = stores;
		this.unmodifiableStores = AoCollections.optimalUnmodifiableList(Arrays.asList(stores));
	}

	public List<ResourceStore> getStores() {
		return unmodifiableStores;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("union(");
		for(int i = 0; i < stores.length; i++) {
			ResourceStore store = stores[i];
			if(i > 0) sb.append(", ");
			sb.append(store.toString());
		}
		return sb.append("):").toString();
	}

	/**
	 * @implSpec  Searches all stores in-order, returning the first one that {@link Resource#exists() exists}.
	 *            If none exist, returns {@link EmptyResource}
	 */
	@Override
	public UnionResource getResource(String path) {
		Resource[] resources = new Resource[stores.length];
		for(int i = 0; i < stores.length; i++) {
			resources[i] = stores[i].getResource(path);
		}
		return new UnionResource(this, path, resources);
	}
}
