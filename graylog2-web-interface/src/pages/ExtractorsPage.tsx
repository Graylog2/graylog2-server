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
import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';

import { DocumentTitle, Spinner } from 'components/common';
import PageHeader from 'components/common/PageHeader';
import ExtractorsList from 'components/extractors/ExtractorsList';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import withParams from 'routing/withParams';
import { InputsActions } from 'stores/inputs/InputsStore';
import { NodesActions, NodesStore } from 'stores/nodes/NodesStore';
import { useStore } from 'stores/connect';
import useParams from 'routing/useParams';

const ExtractorsPage = () => {
  const params = useParams<{ inputId: string, nodeId: string }>();
  const node = useStore(NodesStore, (nodes) => (params.nodeId
    ? nodes.nodes?.[params.nodeId]
    : Object.values(nodes.nodes).filter((_node) => _node.is_leader)[0]));
  const { data: input } = useQuery(['input', params.inputId], () => InputsActions.get(params.inputId));

  useEffect(() => {
    NodesActions.list();
  }, []);

  const _isLoading = !(input && node);

  if (_isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={`Extractors of ${input.title}`}>
      <div>
        <PageHeader title={<span>Extractors of <em>{input.title}</em></span>}
                    actions={(
                      <DropdownButton bsStyle="info" id="extractor-actions-dropdown" title="Actions" pullRight>
                        <MenuItem href={Routes.import_extractors(node.node_id, input.id)}>Import extractors</MenuItem>
                        <MenuItem href={Routes.export_extractors(node.node_id, input.id)}>Export extractors</MenuItem>
                      </DropdownButton>
                      )}
                    documentationLink={{
                      title: 'Extractors documentation',
                      path: DocsHelper.PAGES.EXTRACTORS,
                    }}>
          <span>
            Extractors are applied on every message that is received by this input. Use them to extract and transform{' '}
            any text data into fields that allow you easy filtering and analysis later on.{' '}
            Example: Extract the HTTP response code from a log message, transform it to a numeric field and attach it{' '}
            as <em>http_response_code</em> to the message.
          </span>
        </PageHeader>
        <ExtractorsList input={input} node={node} />
      </div>
    </DocumentTitle>
  );
};

export default withParams(ExtractorsPage);
