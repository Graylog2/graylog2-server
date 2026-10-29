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

import { NodeMaintenanceDropdown, NodeOverview } from 'components/nodes';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { fetchInputStates } from 'hooks/useInputsStates';
import useInputTypesDescriptions from 'hooks/useInputTypesDescriptions';
import useClusterOverview, { fetchNodeJvm } from 'hooks/useClusterOverview';
import { NodesStore } from 'stores/nodes/NodesStore';
import useParams from 'routing/useParams';
import { useStore } from 'stores/connect';
import usePluginList from 'hooks/usePluginList';
import useProductName from 'brand-customization/useProductName';

const ShowNodePage = () => {
  const { t } = useTranslation('system');
  const productName = useProductName();
  const { nodeId } = useParams<{ nodeId: string }>();
  const { data: inputDescriptions } = useInputTypesDescriptions();
  const { nodes } = useStore(NodesStore);
  const { data: clusterOverview } = useClusterOverview();
  const { pluginList, isLoading: isLoadingPlugins } = usePluginList(nodeId);
  const { data: jvmInformation } = useQuery({
    queryKey: ['jvm', nodeId],
    queryFn: () => fetchNodeJvm(nodeId),
  });
  const { data: inputStates } = useQuery({
    queryKey: ['inputs', 'states', nodeId],
    queryFn: () =>
      fetchInputStates().then((allInputStates) => {
        const filteredInputStates = [];

        Object.keys(allInputStates).forEach((inputId) => {
          const inputObject = allInputStates[inputId][nodeId];

          if (inputObject) {
            filteredInputStates.push(inputObject);
          }
        });

        return filteredInputStates;
      }),
  });

  const systemOverview = clusterOverview?.[nodeId];
  const node = nodes?.[nodeId];
  const _isLoading = !node || !systemOverview || isLoadingPlugins;

  if (_isLoading) {
    return <Spinner />;
  }

  const title = (
    <span>{t('node_page.title', { shortNodeId: node.short_node_id, hostname: node.hostname })}</span>
  );

  return (
    <DocumentTitle title={t('node_page.title', { shortNodeId: node.short_node_id, hostname: node.hostname })}>
      <div>
        <PageHeader title={title} actions={<NodeMaintenanceDropdown node={node} />}>
          <span>
            {t('node_page.description', { productName })}
            <br />
            {node.is_leader ? (
              <span>{t('node_page.leader_node')}</span>
            ) : (
              <span>
                <Trans t={t} i18nKey="node_page.not_leader_node" components={{ em: <em /> }} />
              </span>
            )}
          </span>
        </PageHeader>
        <NodeOverview
          node={node}
          systemOverview={systemOverview}
          jvmInformation={jvmInformation}
          plugins={pluginList.plugins}
          inputStates={inputStates}
          inputDescriptions={inputDescriptions as any}
        />
      </div>
    </DocumentTitle>
  );
};

export default ShowNodePage;
