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

import { Button } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import useDataNodes from 'components/datanode/hooks/useDataNodes';

type Props = {
    onStepComplete: () => void,
};

const Welcome = ({ onStepComplete } : Props) => {
  const { data: dataNodes } = useDataNodes();

  return (
    <>
      <h3>Welcome</h3>
      <p>Using the Remote Re-Indexing will allow you to move the datanode by reindexing the data in your existing cluster to the datanode cluster.</p>
      <p>To start please install Data node on every OS/ES node from you previous setup. You can fing more information on how to download and install the data node  <DocumentationLink page="graylog-data-node" text="here" />.</p>
      <MigrationDatanodeList dataNodes={dataNodes} />
      <Button bsStyle="primary" disabled={!dataNodes} bsSize="small" onClick={() => onStepComplete()}>Next</Button>
    </>
  );
};

export default Welcome;
