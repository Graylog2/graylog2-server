import React from 'react';

import { Button } from 'components/bootstrap';
import type { MigrationActions } from 'components/datanode/Types';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import useDataNodes from 'components/datanode/hooks/useDataNodes';

type Props = {
    onStepComplete: (step: {step: MigrationActions}) => void,
    nextSteps: Array<MigrationActions>
};

const Welcome = ({ onStepComplete, nextSteps } : Props) => {
  const { data: dataNodes } = useDataNodes();

  return (
    <>
      <h3>Welcome</h3>
      <p>Using the rolling upgrade will allow you to move the datanode by reindexing the data in your existing cluster to the datanode cluster.</p>
      <p>To start please install Data node on every OS/ES node from you previous setup. You can fing more information on how to download and install the data node  <DocumentationLink page="graylog-data-node" text="here" />.</p>
      <MigrationDatanodeList dataNodes={dataNodes} />
      <Button bsStyle="primary" disabled={!dataNodes} bsSize="small" onClick={() => onStepComplete({ step: nextSteps[0] })}>Next</Button>
    </>
  );
};

export default Welcome;
