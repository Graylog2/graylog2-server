import PropTypes from 'prop-types';
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

export const SystemOverviewSummary = ({ information }) => {
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

SystemOverviewSummary.propTypes = {
  information: PropTypes.object.isRequired,
};

export default SystemOverviewSummary;
