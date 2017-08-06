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

import com.semanticcms.core.resources.Resource;
import com.semanticcms.core.resources.ResourceConnection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a {@link ResourceConnection} to refer back to the {@link UnionResource}
 * instead of the individual underlying {@link Resource}.
 */
public class UnionResourceConnection extends ResourceConnection {

	private final ResourceConnection wrapped;

	public UnionResourceConnection(UnionResource resource, ResourceConnection wrapped) {
		super(resource);
		this.wrapped = wrapped;
	}

	@Override
	public UnionResource getResource() {
		return (UnionResource)resource;
	}

	@Override
	public boolean exists() throws IOException, IllegalStateException {
		return wrapped.exists();
	}

	@Override
	public long getLength() throws IOException, FileNotFoundException, IllegalStateException {
		return wrapped.getLength();
	}

	@Override
	public long getLastModified() throws IOException, FileNotFoundException, IllegalStateException {
		return wrapped.getLastModified();
	}

	@Override
	public InputStream getInputStream() throws IOException, FileNotFoundException, IllegalStateException {
		return wrapped.getInputStream();
	}

	@Override
	public File getFile() throws IOException, FileNotFoundException, IllegalStateException {
		return wrapped.getFile();
	}

	@Override
	public void close() throws IOException {
		wrapped.close();
	}
}
