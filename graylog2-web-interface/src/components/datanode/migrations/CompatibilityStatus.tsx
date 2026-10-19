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
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';

import type { NodeInfo } from 'components/datanode/Types';
import { Accordion, AccordionItem, Timestamp } from 'components/common';
import { Table } from 'components/bootstrap';

type Props = {
  hostname: string;
  opensearchVersion: string;
  nodeInfo: {
    nodes: Array<NodeInfo>;
    opensearch_data_location: string;
  };
};

const Grid = styled.div`
  display: grid;
  grid-template-columns: 25% 75%;
  gap: 0.5rem;
  margin-bottom: 1rem;
`;

const StyledSpan = styled.span`
  display: block;
  clear: both;
`;

const CompatibilityStatus = ({ hostname, opensearchVersion, nodeInfo }: Props) => {
  const { t } = useTranslation('datanode');
  const { opensearch_data_location: opensearchLocation, nodes } = nodeInfo;
  const [activeAccordion, setActiveAccordion] = useState<string | undefined>(
    `Node: 1, Version: ${nodes[0]?.node_version}, ${nodes[0]?.indices.length} indices`,
  );

  const handleSelect = (nextKey: string | undefined) => {
    setActiveAccordion(nextKey ?? activeAccordion);
  };

  return (
    <Grid>
      <div>
        <StyledSpan>
          <strong>{t('compatibility_status.datanode_opensearch_version')}</strong>: {opensearchVersion}
        </StyledSpan>
        <StyledSpan>
          <strong>{t('compatibility_status.opensearch_data_location')}</strong>: {opensearchLocation}
        </StyledSpan>
      </div>
      <div>
        <Accordion
          defaultActiveKey={activeAccordion}
          onSelect={handleSelect}
          id="nodes"
          data-testid="nodes"
          activeKey={activeAccordion}>
          {nodes.map((node) => (
            <AccordionItem
              key={`${node.node_version}${node.indices.length}`}
              id={`node-${hostname}-${node.node_version}-${node.indices.length}`}
              name={t('compatibility_status.node_title', {
                hostname,
                version: node.node_version,
                count: node.indices.length,
              })}>
              <Table bordered condensed>
                <thead>
                  <tr>
                    <th>{t('compatibility_status.indices')}</th>
                    <th>{t('compatibility_status.creation_date')}</th>
                    <th>{t('compatibility_status.index_version')}</th>
                    <th>{t('compatibility_status.shards')}</th>
                  </tr>
                </thead>
                <tbody>
                  {node.indices.map((indice) => (
                    <tr key={indice.index_id}>
                      <td>{indice.index_name}</td>
                      <td>
                        <Timestamp dateTime={indice.creation_date} />
                      </td>
                      <td>{indice.index_version_created}</td>
                      <td>{t('compatibility_status.shard_count', { count: indice.shards.length })}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </AccordionItem>
          ))}
        </Accordion>
      </div>
    </Grid>
  );
};

export default CompatibilityStatus;
