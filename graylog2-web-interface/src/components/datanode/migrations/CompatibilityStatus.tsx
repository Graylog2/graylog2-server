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
import { useState } from 'react';

import type { NodeInfo } from 'components/datanode/Types';
import { Accordion, AccordionItem, Pluralize, Timestamp } from 'components/common';
import { Table } from 'components/bootstrap';

type Props = {
  hostname: string,
  opensearchVersion: string,
  nodeInfo: {
    nodes: Array<NodeInfo>,
    opensearch_data_location: string,
  }
};

const Grid = styled.div`
  display: grid;
  grid-template-columns: 25% 75%;
  grid-gap: 0.5rem;
  margin-bottom: 1rem;
`;

const StyledSpan = styled.span`
  display: block;
  clear: both;
`;

const CompatibilityStatus = ({ hostname, opensearchVersion, nodeInfo }: Props) => {
  const { opensearch_data_location: opensearchLocation, nodes } = nodeInfo;
  const [activeAccordion, setActiveAccordion] = useState<string | undefined>(`Node: 1, Version: ${nodes[0]?.node_version}, ${nodes[0]?.indices.length} indices`);

  const handleSelect = (nextKey: string | undefined) => {
    setActiveAccordion(nextKey ?? activeAccordion);
  };

  return (
    <Grid>
      <div>
        <StyledSpan><strong>Datanode OpenSearch version</strong>: {opensearchVersion}</StyledSpan>
        <StyledSpan><strong>OpenSearch data location</strong>: {opensearchLocation}</StyledSpan>
      </div>
      <div>
        <Accordion defaultActiveKey={activeAccordion}
                   onSelect={handleSelect}
                   id="nodes"
                   data-testid="nodes"
                   activeKey={activeAccordion}>
          {nodes.map((node) => (
            <AccordionItem key={`${node.node_version}${node.indices.length}`} name={`Node: ${hostname}, Version: ${node.node_version}, ${node.indices.length} indices`}>
              <Table striped bordered condensed>
                <thead>
                  <tr>
                    <th>Indices</th>
                    <th>Creation date</th>
                    <th>Index version</th>
                    <th>Shards</th>
                  </tr>
                </thead>
                <tbody>
                  {node.indices.map((indice) => (
                    <tr key={indice.index_id}>
                      <td>{indice.index_name}</td>
                      <td><Timestamp dateTime={indice.creation_date} /></td>
                      <td>{indice.index_version_created}</td>
                      <td>{indice.shards.length} <Pluralize singular="shard" plural="shards" value={indice.shards.length} /> </td>
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
