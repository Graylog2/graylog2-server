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
import { useNavigate } from 'react-router-dom';

import Routes from 'routing/Routes';
import usePluginEntities from 'hooks/usePluginEntities';
import { Row, Col, Button, Label } from 'components/bootstrap';
import useScopePermissions from 'hooks/useScopePermissions';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import type { CachePluginType } from './types';
import { SummaryRow, Title, Value } from './caches/SummaryComponents.styled';

type Props = {
  cache: LookupTableCache;
  noEdit?: boolean;
};

const Cache = ({ cache, noEdit = false }: Props) => {
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);
  const plugin = usePluginEntities('lookupTableCaches').find((p: CachePluginType) => p.type === cache.config?.type);
  const navigate = useNavigate();

  const canEdit = !noEdit && !loadingScopePermissions && scopePermissions?.is_mutable;

  if (!plugin) {
    return <p>Unknown cache type {cache.config.type}. Is the plugin missing?</p>;
  }

  const handleEdit = () => {
    navigate(Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cache.name));
  };

  return (
    <Row className="content">
      <Col md={12}>
        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <Label>{plugin.displayName}</Label>
          {canEdit && (
            <Button bsStyle="primary" onClick={handleEdit} role="button" name="edit_square">
              Edit
            </Button>
          )}
        </div>

        <SummaryRow>
          <Title>Description:</Title>
          <Value>{cache.description || <em>No description.</em>}</Value>
        </SummaryRow>

        <h4>Configuration</h4>
        <div>{React.createElement(plugin.summaryComponent, { cache: cache })}</div>
      </Col>
    </Row>
  );
};

export default Cache;
