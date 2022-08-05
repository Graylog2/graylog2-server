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
import { useHistory } from 'react-router-dom';

import usePluginEntities from 'views/logic/usePluginEntities';
import Routes from 'routing/Routes';
import { Row, Col, Button } from 'components/bootstrap';
import useScopePermissions from 'hooks/useScopePermissions';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import type { CachePluginType } from './types';
import {
  SummaryContainer,
  SummaryRow,
  Title,
  Value,
} from './caches/SummaryComponents.styled';

type Props = {
  cache: LookupTableCache,
};

const Cache = ({ cache }: Props) => {
  const history = useHistory();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);
  const plugin = usePluginEntities('lookupTableCaches').find((p: CachePluginType) => p.type === cache.config?.type);

  if (!plugin) {
    return <p>Unknown cache type {cache.config.type}. Is the plugin missing?</p>;
  }

  const handleEdit = (cacheName: string) => () => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cacheName));
  };

  return (
    <Row className="content">
      <Col md={12}>
        <h2>{cache.title} <small>({plugin.displayName})</small></h2>
        <SummaryContainer>
          <SummaryRow>
            <Title>Description:</Title>
            <Value>{cache.description || <em>No description.</em>}</Value>
          </SummaryRow>
        </SummaryContainer>
        <h4>Configuration</h4>
        <div>{React.createElement(plugin.summaryComponent, { cache: cache })}</div>
        {(!loadingScopePermissions && scopePermissions?.is_mutable) && (
          <Button bsStyle="success"
                  onClick={handleEdit(cache.name)}
                  role="button"
                  name="edit">
            Edit
          </Button>
        )}
      </Col>
    </Row>
  );
};

export default Cache;
