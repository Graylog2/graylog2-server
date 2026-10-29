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
import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Trans, useTranslation } from 'react-i18next';

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Timestamp } from 'components/common';
import { fetchProcessBufferDump } from 'hooks/useClusterOverview';
import { NodesStore } from 'stores/nodes/NodesStore';
import useParams from 'routing/useParams';
import { useStore } from 'stores/connect';

const ProcessBufferDumpPage = () => {
  const { t } = useTranslation('system');
  const { nodeId } = useParams<{ nodeId: string }>();
  const { nodes } = useStore(NodesStore);
  const { data: processbufferDump } = useQuery({
    queryKey: ['processBufferDump', nodeId],

    queryFn: () => fetchProcessBufferDump(nodeId),
  });

  const node = nodes?.[nodeId];

  if (!node) {
    return <Spinner />;
  }

  const title = (
    <span>
      {t('process_buffer_dump_page.title', { shortNodeId: node.short_node_id, hostname: node.hostname })}
      &nbsp;
      <small>
        <Trans
          t={t}
          i18nKey="process_buffer_dump_page.taken_at"
          components={[<Timestamp dateTime={new Date()} />]}
        />{' '}
      </small>
    </span>
  );

  const content = processbufferDump ? (
    <pre className="processbufferdump">{JSON.stringify(processbufferDump, null, 2)}</pre>
  ) : (
    <Spinner />
  );

  return (
    <DocumentTitle
      title={t('process_buffer_dump_page.title', { shortNodeId: node.short_node_id, hostname: node.hostname })}>
      <div>
        <PageHeader title={title} />
        <Row className="content">
          <Col md={12}>{content}</Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default ProcessBufferDumpPage;
