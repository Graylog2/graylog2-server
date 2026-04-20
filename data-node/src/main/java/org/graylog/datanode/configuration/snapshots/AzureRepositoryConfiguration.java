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
package org.graylog.datanode.configuration.snapshots;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreStringItem;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@DocumentationSection(heading = "Azure repository configuration for searchable snapshots", description = "")
public class AzureRepositoryConfiguration implements RepositoryConfiguration {

    @Documentation("Azure client default account for searchable snapshots")
    @Parameter(value = "azure_client_default_account")
    private String azureClientDefaultAccount;

    @Documentation("Azure client default account for searchable snapshots")
    @Parameter(value = "azure_client_default_key")
    private String azureClientDefaultKey;

    @Documentation("Azure client default account for searchable snapshots")
    @Parameter(value = "azure_client_default_sas_token")
    private String azureClientDefaultSasToken;

    @Override
    public boolean isRepositoryEnabled() {
        if (noneBlank(azureClientDefaultAccount) && noneBlank(azureClientDefaultSasToken) || noneBlank(azureClientDefaultKey)) {
            // All the required properties are set and not blank, s3 repository is enabled
            return true;
        } else if (allBlank(azureClientDefaultAccount, azureClientDefaultKey, azureClientDefaultSasToken)) {
            // all are empty, this means repository is not configured at all
            return false;
        } else {
            // One or two properties are configured, this is an incomplete configuration we can't handle this situation
            throw new IllegalStateException("""
                    Azure Client not configured properly,
                    Property azure_client_default_account is required, together with either azure_client_default_key or azure_client_default_sas_token
                    , they have to be provided in the configuration!""");
        }
    }

    @Override
    public Map<String, String> opensearchProperties() {
        return Collections.emptyMap();
    }

    @Override
    public Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories) {
        final ImmutableList.Builder<OpensearchKeystoreItem> builder = ImmutableList.builder();
        builder.add(new OpensearchKeystoreStringItem("azure.client.default.account", azureClientDefaultAccount));
        if (!StringUtils.isEmpty(azureClientDefaultKey)) {
            builder.add(new OpensearchKeystoreStringItem("azure.client.default.key", azureClientDefaultKey));
        } else if (!StringUtils.isEmpty(azureClientDefaultSasToken)) {
            builder.add(new OpensearchKeystoreStringItem("azure.client.default.sas_token", azureClientDefaultSasToken));
        }
        return builder.build();
    }
}
