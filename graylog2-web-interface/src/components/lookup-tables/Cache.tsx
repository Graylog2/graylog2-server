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
import { PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import { Row, Col, Button } from 'components/bootstrap';
import useScopePermissions from 'hooks/useScopePermissions';
import type { LookupTableCache } from 'logic/lookup-tables/types';

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
  const { getScopePermissions } = useScopePermissions();
  const plugins = {};

  PluginStore.exports('lookupTableCaches').forEach((p: any) => {
    plugins[p.type] = p;
  });

  const plugin = plugins[cache.config.type];

  if (!plugin) {
    return <p>Unknown cache type {cache.config.type}. Is the plugin missing?</p>;
  }

  const summary = plugin.summaryComponent;

  const handleEdit = (cacheName: string) => () => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cacheName));
  };

  const showAction = (inCache: LookupTableCache): boolean => {
    const permissions = getScopePermissions(inCache._scope);

    return permissions.is_mutable;
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
        <div>{React.createElement(summary, { cache: cache })}</div>
        {showAction(cache) && (
          <Button bsStyle="success" onClick={handleEdit(cache.name)} alt="edit button">Edit</Button>
        )}
      </Col>
    </Row>
  );
};

export default Cache;
