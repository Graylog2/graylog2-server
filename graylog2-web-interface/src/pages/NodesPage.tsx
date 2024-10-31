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
import URI from 'urijs';

import * as URLUtils from 'util/URLUtils';
import { DocumentTitle, ExternalLinkButton, PageHeader, Spinner } from 'components/common';
import { NodesList } from 'components/nodes';
import type { NodeInfo } from 'stores/nodes/NodesStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import { useStore } from 'stores/connect';

const GLOBAL_API_BROWSER_URL = '/api-browser/global/index.html';

const hasExternalURI = (nodes: { [nodeId: string]: NodeInfo }) => {
  const nodeVals = Object.values(nodes);
  const publishURI = URLUtils.qualifyUrl('/');

  return (nodeVals.findIndex((node) => new URI(node.transport_address).normalizePathname().toString() !== publishURI) >= 0);
};

const GlobalAPIButton = ({ nodes }: { nodes: { [nodeId: string]: NodeInfo } }) => {
  if (!nodes) {
    return <Spinner />;
  }

  if (hasExternalURI(nodes)) {
    return (
      <ExternalLinkButton bsStyle="info" href={URLUtils.qualifyUrl(GLOBAL_API_BROWSER_URL)}>
        Cluster Global API browser
      </ExternalLinkButton>
    );
  }

  return null;
};

const NodesPage = () => {
  const { nodes } = useStore(NodesStore);

  return (
    <DocumentTitle title="Nodes">
      <div>
        <PageHeader title="Nodes" actions={<GlobalAPIButton nodes={nodes} />}>
          <span>
            This page provides a real-time overview of the nodes in your Graylog cluster.
            You can pause message processing at any time. The process buffers will not accept any new messages until
            you resume it. If the message journal is enabled for a node, which it is by default, incoming messages
            will be persisted to disk, even when processing is disabled.
          </span>
        </PageHeader>
        <NodesList nodes={nodes} />
      </div>
    </DocumentTitle>
  );
};

export default NodesPage;
