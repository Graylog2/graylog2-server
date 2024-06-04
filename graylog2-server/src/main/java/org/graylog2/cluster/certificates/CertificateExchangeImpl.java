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
package org.graylog2.cluster.certificates;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog2.database.MongoConnection;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class CertificateExchangeImpl implements CertificateExchange {

    private static final String ENCRYPTED_VALUE_SUBFIELD = "encrypted_value";
    private static final String SALT_SUBFIELD = "salt";

    private static final String COLLECTION_NAME = "certificate_exchange";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_ENTRY_TYPE = "type";
    public static final String FIELD_ENCRYPTED_VALUE = "value";

    private final MongoDatabase mongoDatabase;
    private final EncryptedValueService encryptionService;

    @Inject
    public CertificateExchangeImpl(final MongoConnection mongoConnection, EncryptedValueService encryptionService) {
        this.mongoDatabase = mongoConnection.getMongoDatabase();
        this.encryptionService = encryptionService;
        createIndex(this.mongoDatabase);
    }

    private static void createIndex(MongoDatabase mongoDatabase) {
        final MongoCollection<Document> dbCollection = mongoDatabase.getCollection(COLLECTION_NAME);
        dbCollection.createIndex(ascending(FIELD_NODE_ID, FIELD_ENTRY_TYPE), new IndexOptions().unique(true));
    }

    private void writeCertificate(String nodeId, CertificateChain certificateChain) throws IOException {
        writeToDatabase(nodeId, toString(certificateChain), CertificateExchangeType.CERT_CHAIN);
    }

    private void writeToDatabase(String nodeId, String serializedValue, CertificateExchangeType type) {
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(COLLECTION_NAME);
        final EncryptedValue encrypted = encryptionService.encrypt(serializedValue);
        final UpdateResult result = dbCollection.updateOne(
                Filters.and(
                        eq(FIELD_NODE_ID, nodeId),
                        eq(FIELD_ENTRY_TYPE, type.name())
                ),
                combine(
                        set(FIELD_NODE_ID, nodeId),
                        set(FIELD_ENCRYPTED_VALUE + "." + ENCRYPTED_VALUE_SUBFIELD, encrypted.value()),
                        set(FIELD_ENCRYPTED_VALUE + "." + SALT_SUBFIELD, encrypted.salt())
                ),
                new UpdateOptions().upsert(true)
        );
        final boolean updated = result.getModifiedCount() > 0 || result.getUpsertedId() != null;
        if (!updated) {
            throw new RuntimeException("Failed to write certificateChain");
        }
    }

    @Override
    public void signPendingCertificateRequests(Function<CertificateSigningRequest, CertificateChain> signingFunction) throws IOException {
        Optional<CertificateSigningRequest> req = getNextCertificateSigningRequest();
        while (req.isPresent()) {
            final CertificateSigningRequest csr = req.get();
            writeCertificate(csr.nodeId(), signingFunction.apply(csr));
            removeCertificateSigningRequest(csr.nodeId());
            req = getNextCertificateSigningRequest();
        }
    }

    @Override
    public void requestCertificate(CertificateSigningRequest request) throws IOException {
        writeToDatabase(request.nodeId(), toString(request.request()), CertificateExchangeType.CSR);
    }

    @Override
    public void pollCertificate(String nodeId, Consumer<CertificateChain> chainConsumer) {
        readEntry(
                Filters.and(
                        eq(FIELD_NODE_ID, nodeId),
                        eq(FIELD_ENTRY_TYPE, CertificateExchangeType.CERT_CHAIN.name())), (document, decryptedValue) -> parseCertificateChain(decryptedValue))
                .ifPresent(certificateChain -> {
                    chainConsumer.accept(certificateChain);
                    removeCertificateChain(nodeId);
                });
    }

    @NotNull
    private <T> Optional<T> readEntry(Bson filter, BiFunction<Document, String, T> parser) {
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(COLLECTION_NAME);
        final FindIterable<Document> objects = dbCollection.find(
                filter
        );
        final Document document = objects.first();

        if (document != null) {
            final Document encryptedCertificateDocument = document.get(FIELD_ENCRYPTED_VALUE, Document.class);
            if (encryptedCertificateDocument != null) {
                final EncryptedValue encryptedCertificate = EncryptedValue.builder()
                        .value(encryptedCertificateDocument.getString(ENCRYPTED_VALUE_SUBFIELD))
                        .salt(encryptedCertificateDocument.getString(SALT_SUBFIELD))
                        .isDeleteValue(false)
                        .isKeepValue(false)
                        .build();

                final String decryptedValue = encryptionService.decrypt(encryptedCertificate);
                return Optional.ofNullable(parser.apply(document, decryptedValue));
            }
        }
        return Optional.empty();
    }

    private Optional<CertificateSigningRequest> getNextCertificateSigningRequest() {
        return readEntry(eq(FIELD_ENTRY_TYPE, CertificateExchangeType.CSR.name()), (document, decryptedValue) -> new CertificateSigningRequest(document.getString(FIELD_NODE_ID), parseCSR(decryptedValue)));
    }

    private boolean removeCertificateChain(String nodeId) {
        return removeEntry(nodeId, CertificateExchangeType.CERT_CHAIN);
    }

    private boolean removeCertificateSigningRequest(String nodeId) {
        return removeEntry(nodeId, CertificateExchangeType.CSR);
    }

    private boolean removeEntry(String nodeId, CertificateExchangeType type) {
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(COLLECTION_NAME);
        var result = dbCollection.deleteOne(
                Filters.and(
                        eq(FIELD_NODE_ID, nodeId),
                        eq(FIELD_ENTRY_TYPE, type.name())
                )
        );
        return result.getDeletedCount() > 0;
    }


    private static String toString(PKCS10CertificationRequest csr) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(csr);
        }
        return writer.toString();
    }

    private static String toString(CertificateChain certChain) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            for (Certificate c : certChain.toCertificateChainArray()) {
                jcaPEMWriter.writeObject(c);
            }
        }
        return writer.toString();
    }

    public CertificateChain parseCertificateChain(String certificate) {
        try {
            Reader pemReader = new BufferedReader(new StringReader(certificate));
            PEMParser pemParser = new PEMParser(pemReader);
            List<X509Certificate> caCerts = new LinkedList<>();
            X509Certificate signedCert = readSingleCert(pemParser);

            X509Certificate caCert = readSingleCert(pemParser);
            while (caCert != null) {
                caCerts.add(caCert);
                caCert = readSingleCert(pemParser);
            }
            return new CertificateChain(signedCert, caCerts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private X509Certificate readSingleCert(PEMParser pemParser) throws IOException, CertificateException {
        var parsedObj = pemParser.readObject();
        if (parsedObj == null) {
            return null;
        }
        if (parsedObj instanceof X509Certificate) {
            return (X509Certificate) parsedObj;
        } else if (parsedObj instanceof X509CertificateHolder) {
            return new JcaX509CertificateConverter().getCertificate(
                    (X509CertificateHolder) parsedObj
            );
        } else {
            throw new IllegalArgumentException("Cannot read certificate from PEMParser. Containing object is of unexpected type " + parsedObj.getClass());
        }
    }

    private PKCS10CertificationRequest parseCSR(String csr) {
        try {
            final var pemReader = new BufferedReader(new StringReader(csr));
            final var pemParser = new PEMParser(pemReader);
            final var parsedObj = pemParser.readObject();
            return (PKCS10CertificationRequest) parsedObj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
