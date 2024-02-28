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
package org.graylog2.bootstrap.preflight.web.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CreateCARequest;
import org.graylog2.bootstrap.preflight.web.resources.model.CreateClientCertRequest;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.ApiError;

import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Path("/ca")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@Api(value = "CA", description = "Certificate Authority", tags = {CLOUD_VISIBLE})
public class CAResource {
    private final CaService caService;
    private final String passwordSecret;

    private final CsrGenerator csrGenerator;
    private final CsrSigner csrSigner;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public CAResource(final CaService caService,
                      final @Named("password_secret") String passwordSecret,
                      final CsrGenerator csrGenerator,
                      final CsrSigner csrSigner,
                      final ClusterConfigService clusterConfigService) {
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.csrGenerator = csrGenerator;
        this.csrSigner = csrSigner;
        this.clusterConfigService = clusterConfigService;
    }

    @GET
    @ApiOperation("Returns the CA")
    public CA get() throws KeyStoreStorageException {
        return caService.get();
    }

    @POST
    @Path("create")
    @NoAuditEvent("No Audit Event needed")
    @ApiOperation("Creates a CA")
    public void createCA(@ApiParam(name = "request", required = true) @NotNull @Valid CreateCARequest request) throws CACreationException, KeyStoreStorageException, KeyStoreException {
        caService.create(request.organization(), CaService.DEFAULT_VALIDITY, passwordSecret.toCharArray());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("upload")
    @NoAuditEvent("No Audit Event needed")
    @ApiOperation("Upload a CA")
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadCA(@ApiParam(name = "password") @FormDataParam("password") String password, @ApiParam(name = "files") @FormDataParam("files") List<FormDataBodyPart> files) {
        try {
            caService.upload(password, files);
            return Response.ok().build();
        } catch (CACreationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }

    @POST
    @Path("createClientCert")
    @NoAuditEvent("No Audit Event needed")
    @ApiOperation("Creates a client certificate")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClientCert(@ApiParam(name = "request", required = true) @NotNull @Valid CreateClientCertRequest request) {
        try {
            var principal = request.principal();
            var password = request.password().toCharArray();
            var renewalPolicy = this.clusterConfigService.get(RenewalPolicy.class);
            var privateKeyEncryptedStorage = new PrivateKeyEncryptedFileStorage(java.nio.file.Path.of(principal + ".cert"));

            final Optional<KeyStore> optKey = caService.loadKeyStore();
            final var caKeystore = optKey.get();

            var caPrivateKey = (PrivateKey) caKeystore.getKey(CA_KEY_ALIAS, passwordSecret.toCharArray());
            var caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);

            var csr = csrGenerator.generateCSR(password, principal, List.of(principal), privateKeyEncryptedStorage);
            var pk = privateKeyEncryptedStorage.readEncryptedKey(password);
            var cert = csrSigner.sign(caPrivateKey, caCertificate, csr, renewalPolicy);

            var writer = new StringWriter();
            try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
                jcaPEMWriter.writeObject(caCertificate);
                jcaPEMWriter.writeObject(pk);
                jcaPEMWriter.writeObject(cert);
            }

            return Response.ok().entity(writer.toString()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
