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
import { useState } from 'react';

import { Button, Table } from 'components/bootstrap';
import { ConfirmDialog, NoEntitiesExist, RelativeTime, Spinner, Timestamp } from 'components/common';
import { Headline } from 'components/common/Section/SectionComponent';
import useProductName from 'brand-customization/useProductName';

import type { McpClientGrant } from './useMcpClientGrants';
import useMcpClientGrants from './useMcpClientGrants';

const McpClientGrants = () => {
  const { grants, isLoading, revokeGrant, isRevoking } = useMcpClientGrants();
  const productName = useProductName();
  const [grantToRevoke, setGrantToRevoke] = useState<McpClientGrant | undefined>();

  const onConfirmRevoke = () => revokeGrant(grantToRevoke.clientId).finally(() => setGrantToRevoke(undefined));

  return (
    <>
      <Headline>Connected MCP Clients</Headline>
      <p>MCP clients you have authorized to access {productName} on your behalf.</p>
      {isLoading && <Spinner />}
      {!isLoading && grants.length === 0 && <NoEntitiesExist>No MCP clients are connected.</NoEntitiesExist>}
      {grants.length > 0 && (
        <Table bordered condensed>
          <thead>
            <tr>
              <th>Client</th>
              <th>Last Token Refresh</th>
              <th>Authorization Expires</th>
              <th className="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {grants.map((grant) => (
              <tr key={grant.clientId}>
                <td>{grant.clientName}</td>
                <td>
                  <RelativeTime dateTime={grant.lastRefreshedAt} />
                </td>
                <td>
                  <Timestamp dateTime={grant.expiresAt} />
                </td>
                <td className="text-right">
                  <Button bsSize="xs" bsStyle="danger" disabled={isRevoking} onClick={() => setGrantToRevoke(grant)}>
                    Revoke
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
      {grantToRevoke && (
        <ConfirmDialog
          show
          title="Revoke MCP client access"
          onCancel={() => setGrantToRevoke(undefined)}
          onConfirm={onConfirmRevoke}>
          <p>
            You are about to revoke the access of &quot;{grantToRevoke.clientName}&quot;. The client will have to be
            authorized again. Are you sure?
          </p>
        </ConfirmDialog>
      )}
    </>
  );
};

export default McpClientGrants;
