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
import { useTranslation } from 'react-i18next';

import useHistory from 'routing/useHistory';
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
  const { t } = useTranslation('lookup_tables');
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);
  const plugin = usePluginEntities('lookupTableCaches').find((p: CachePluginType) => p.type === cache.config?.type);
  const { push } = useHistory();

  const canEdit = !noEdit && !loadingScopePermissions && scopePermissions?.is_mutable;

  if (!plugin) {
    return <p>{t('cache_summary.unknown_type', { type: cache.config.type })}</p>;
  }

  const handleEdit = () => {
    push(Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cache.name));
  };

  return (
    <Row className="content">
      <Col md={12}>
        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <Label>{plugin.displayName}</Label>
          {canEdit && (
            <Button bsStyle="primary" onClick={handleEdit} role="button" name="edit_square">
              {t('common:actions.edit')}
            </Button>
          )}
        </div>

        <SummaryRow>
          <Title>{t('shared.description_colon')}</Title>
          <Value>{cache.description || <em>{t('shared.no_description')}</em>}</Value>
        </SummaryRow>

        <h4>{t('shared.configuration')}</h4>
        <div>{React.createElement(plugin.summaryComponent, { cache: cache })}</div>
      </Col>
    </Row>
  );
};

export default Cache;
