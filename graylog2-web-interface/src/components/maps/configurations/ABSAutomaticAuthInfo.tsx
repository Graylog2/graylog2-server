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
import * as React from 'react';
import styled from 'styled-components';

import { Icon } from 'components/common';
import { Alert, Table } from 'components/bootstrap';

const SectionTitle = styled.p`
  font-weight: bold;
  font-size: ${({ theme }) => theme.fonts.size.h6 };
  margin: 0 0 ${({ theme }) => theme.spacings.sm };
`;

const SectionNote = styled.p`
  font-style: italic;
  margin: ${({ theme }) => theme.spacings.xxs } 0 0;
`;

const StyledTable = styled(Table)`
  margin: 0;
`;

const EnvVarList = styled.div`
  margin-top: ${({ theme }) => theme.spacings.xxs };
`;

const ABSAutomaticAuthInfo = () => (
  <Alert bsStyle="info">
    <StyledTable condensed>
      <thead>
        <tr>
          <td colSpan={2}>
            <SectionTitle>Automatic authentication will attempt each of the following in the listed order.</SectionTitle>
          </td>
        </tr>
      </thead>

      <tbody>
        <tr>
          <th>Environment</th>
          <td>
            Authenticates via environment variables.
            <EnvVarList>
              The required environment variables for service principal authentication using client secret are as follows:{' '}
              <code>AZURE_CLIENT_ID</code>, <code>AZURE_CLIENT_SECRET</code>, <code>AZURE_TENANT_ID</code>
            </EnvVarList>
            <EnvVarList>
              The required environment variables for service principal authentication using client certificate are as
              follows: <code>AZURE_CLIENT_ID</code>, <code>AZURE_CLIENT_CERTIFICATE_PATH</code>,{' '}
              <code>AZURE_CLIENT_CERTIFICATE_PASSWORD</code>, <code>AZURE_TENANT_ID</code>
            </EnvVarList>
          </td>
        </tr>
        <tr>
          <th>Workload Identity</th>
          <td>For use on an Azure host with Workload Identity enabled.</td>
        </tr>
        <tr>
          <th>Managed Identity</th>
          <td>For use on an Azure host with Managed Identity enabled.</td>
        </tr>
        <tr>
          <th>Azure CLI</th>
          <td>Uses current Azure CLI account.</td>
        </tr>
        <tr>
          <th>Broker</th>
          <td>
            Use the default account logged into the OS via a broker. Requires that the{' '}
            <code>azure-identity-broker</code> package is installed.
          </td>
        </tr>
      </tbody>

      <tfoot>
        <tr>
          <td colSpan={2}>
            <SectionNote>
              For more information, check out the{' '}
              <a
                href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#defaultazurecredential"
                target="_blank"
                rel="noopener noreferrer">
                Azure DefaultAzureCredential Documentation <Icon name="open_in_new" />
              </a>
            </SectionNote>
          </td>
        </tr>
      </tfoot>
    </StyledTable>
  </Alert>
);

export default ABSAutomaticAuthInfo;
