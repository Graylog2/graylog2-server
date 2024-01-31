import React from 'react';

import type { MigrationActions } from 'components/datanode/Types';
import { Button } from 'components/bootstrap';

type Props = {
  onStepComplete: (step: {step: MigrationActions}) => void,
  nextSteps: Array<MigrationActions>
};

const ConnectionStringRemoval = ({ onStepComplete, nextSteps }: Props) => (
  <>
    <p>Please remove the <code>elasticsearch_hosts</code> line from you graylog</p>
    <p>Ex. <code>elasticsearch_hosts = https://admin:admin@opensearch1:9200,https://admin:admin@opensearch2:9200,https://admin:admin@opensearch3:9200</code></p>
    <Button bsStyle="primary" bsSize="small" onClick={() => onStepComplete({ step: nextSteps[0] })}>Next</Button>
  </>
);
export default ConnectionStringRemoval;
