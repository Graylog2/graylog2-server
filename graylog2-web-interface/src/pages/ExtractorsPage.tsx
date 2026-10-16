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
import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';

import { DocumentTitle, Spinner } from 'components/common';
import PageHeader from 'components/common/PageHeader';
import ExtractorsList from 'components/extractors/ExtractorsList';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import withParams from 'routing/withParams';
import { fetchInput } from 'hooks/useInputs';
import { NodesActions, NodesStore } from 'stores/nodes/NodesStore';
import { useStore } from 'stores/connect';
import useParams from 'routing/useParams';

const ExtractorsPage = () => {
  const { t } = useTranslation('extractors');
  const params = useParams<{ inputId: string; nodeId: string }>();
  const node = useStore(NodesStore, (nodes) => {
    if (!nodes?.nodes) {
      return undefined;
    }

    return params.nodeId
      ? nodes.nodes?.[params.nodeId]
      : Object.values(nodes.nodes).filter((_node) => _node.is_leader)[0];
  });
  const { data: input } = useQuery({
    queryKey: ['input', params.inputId],
    queryFn: () => fetchInput(params.inputId),
  });

  useEffect(() => {
    NodesActions.list();
  }, []);

  const _isLoading = !(input && node);

  if (_isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={t('extractors_of', { title: input.title })}>
      <div>
        <PageHeader
          title={
            <span>
              {t('extractors_of', { title: <em key="title">{input.title}</em> })}
            </span>
          }
          actions={
            <DropdownButton bsStyle="info" id="extractor-actions-dropdown" title={t('actions')} pullRight>
              <MenuItem href={Routes.import_extractors(node.node_id, input.id)}>{t('import_extractors')}</MenuItem>
              <MenuItem href={Routes.export_extractors(node.node_id, input.id)}>{t('export_extractors')}</MenuItem>
            </DropdownButton>
          }
          documentationLink={{
            title: t('documentation_title'),
            path: DocsHelper.PAGES.EXTRACTORS,
          }}>
          <span>
            {t('description', { field: 'http_response_code' })}
          </span>
        </PageHeader>
        <ExtractorsList input={input} node={node} />
      </div>
    </DocumentTitle>
  );
};

export default withParams(ExtractorsPage);
