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
import { useCallback, useMemo, useState } from 'react';
import styled from 'styled-components';

import { DeleteMenuItem } from 'components/bootstrap';
import { ConfirmDialog, Link, RelativeTime } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { Sort } from 'stores/PaginationTypes';

import { fetchPaginatedEnrollmentTokens, enrollmentTokensKeyFn, useCollectorsMutations, useFleets } from '../hooks';
import type { EnrollmentTokenMetadata } from '../types';

const DEFAULT_LAYOUT = {
  entityTableId: 'enrollment-tokens',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'created_at', direction: 'desc' } as Sort,
  defaultDisplayedAttributes: ['name', 'fleet_id', 'created_by', 'created_at', 'expires_at', 'usage_count', 'last_used_at'],
  defaultColumnOrder: ['name', 'fleet_id', 'created_by', 'created_at', 'expires_at', 'usage_count', 'last_used_at'],
};

const ExpiredText = styled.span`
  color: ${({ theme }) => theme.colors.variant.danger};
`;

const ExpiresCell = ({ expiresAt }: { expiresAt: string | null }) => {
  if (!expiresAt) {
    return <span>Never</span>;
  }

  const expDate = new Date(expiresAt);

  if (expDate < new Date()) {
    return <ExpiredText>Expired</ExpiredText>;
  }

  return <RelativeTime dateTime={expiresAt} />;
};

// TODO: Use useBasicUser to resolve the full name and link to the user profile.
//       Currently useBasicUser shows an error toast when the user doesn't exist (deleted).
//       It should gracefully fall back to displaying the username instead.
const CreatedByCell = ({ username }: { username: string }) => <span>{username}</span>;

const customColumnRenderers = (fleetNames: Record<string, string>): ColumnRenderers<EnrollmentTokenMetadata> => ({
  attributes: {
    name: {
      renderCell: (name: string) => <span>{name}</span>,
      width: 0.3,
    },
    fleet_id: {
      renderCell: (_fleetId: string, token: EnrollmentTokenMetadata) => (
        <Link to={Routes.SYSTEM.COLLECTORS.FLEET(token.fleet_id)}>{fleetNames[token.fleet_id] || token.fleet_id}</Link>
      ),
      width: 0.2,
    },
    created_by: {
      renderCell: (_createdBy: unknown, token: EnrollmentTokenMetadata) => (
        <CreatedByCell username={token.created_by.username} />
      ),
      width: 0.15,
    },
    created_at: {
      renderCell: (_createdAt: string, token: EnrollmentTokenMetadata) => <RelativeTime dateTime={token.created_at} />,
      width: 0.15,
    },
    expires_at: {
      renderCell: (_expiresAt: string | null, token: EnrollmentTokenMetadata) => (
        <ExpiresCell expiresAt={token.expires_at} />
      ),
      width: 0.15,
    },
    usage_count: {
      renderCell: (usageCount: number) => <span>{usageCount}</span>,
      staticWidth: 80,
    },
    last_used_at: {
      renderCell: (_lastUsedAt: string | null, token: EnrollmentTokenMetadata) =>
        token.last_used_at ? <RelativeTime dateTime={token.last_used_at} /> : <span>Never</span>,
      width: 0.15,
    },
  },
});

const EnrollmentTokenList = () => {
  const { deleteEnrollmentToken } = useCollectorsMutations();
  const { data: fleets } = useFleets();
  const [deletingToken, setDeletingToken] = useState<EnrollmentTokenMetadata | null>(null);

  const fleetNames = useMemo(() => {
    const map: Record<string, string> = {};

    (fleets ?? []).forEach((f) => {
      map[f.id] = f.name;
    });

    return map;
  }, [fleets]);

  const handleConfirmDelete = useCallback(async () => {
    if (!deletingToken) return;

    await deleteEnrollmentToken(deletingToken.id);
    setDeletingToken(null);
  }, [deletingToken, deleteEnrollmentToken]);

  const entityActions = useCallback(
    (token: EnrollmentTokenMetadata) => (
      <MoreActions>
        <DeleteMenuItem onSelect={() => setDeletingToken(token)} />
      </MoreActions>
    ),
    [],
  );

  const renderers = useMemo(() => customColumnRenderers(fleetNames), [fleetNames]);

  return (
    <>
      <PaginatedEntityTable<EnrollmentTokenMetadata>
        humanName="enrollment tokens"
        tableLayout={DEFAULT_LAYOUT}
        fetchEntities={fetchPaginatedEnrollmentTokens}
        keyFn={enrollmentTokensKeyFn}
        entityAttributesAreCamelCase={false}
        columnRenderers={renderers}
        entityActions={entityActions}
      />
      {deletingToken && (
        <ConfirmDialog
          title="Delete enrollment token"
          show
          onConfirm={handleConfirmDelete}
          onCancel={() => setDeletingToken(null)}>
          Are you sure you want to delete this enrollment token? Collectors using this token will not be able to
          re-enroll.
        </ConfirmDialog>
      )}
    </>
  );
};

export default EnrollmentTokenList;
