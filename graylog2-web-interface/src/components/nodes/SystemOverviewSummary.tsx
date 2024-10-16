import React from 'react';
import styled from 'styled-components';

import StringUtils from 'util/StringUtils';

const NodeState = styled.dl`
  margin-top: 0;
  margin-bottom: 0;

  dt {
    float: left;
  }

  dd {
    margin-left: 180px;
  }
`;

type SystemOverviewSummaryProps = {
  information: any;
};

export const SystemOverviewSummary = ({
  information
}: SystemOverviewSummaryProps) => {
  const lbStatus = information.lb_status.toUpperCase();

  return (
    <NodeState>
      <dt>Current lifecycle state:</dt>
      <dd>{StringUtils.capitalizeFirstLetter(information.lifecycle)}</dd>
      <dt>Message processing:</dt>
      <dd>{information.is_processing ? 'Enabled' : 'Disabled'}</dd>
      <dt>Load balancer indication:</dt>
      <dd className={lbStatus === 'DEAD' ? 'text-danger' : ''}>{lbStatus}</dd>
    </NodeState>
  );
};

export default SystemOverviewSummary;
