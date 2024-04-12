/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.security.certutil;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

public interface CaService {
    int DEFAULT_VALIDITY = 10 * 365;

    CA get() throws KeyStoreStorageException;

    CA create(String organization, final Integer daysValid, char[] password) throws CACreationException, KeyStoreStorageException, KeyStoreException;

    void upload(String pass, List<FormDataBodyPart> parts) throws CACreationException;
    void startOver();
    Optional<KeyStore> loadKeyStore() throws KeyStoreException, KeyStoreStorageException, NoSuchAlgorithmException;
}
