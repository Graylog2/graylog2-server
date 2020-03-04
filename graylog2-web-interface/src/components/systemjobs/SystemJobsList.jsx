import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import { Icon } from 'components/common';
import { SystemJob } from 'components/systemjobs';

const SystemJobWrap = styled.div`
  margin-top: 10px;
  border-bottom: 1px solid #ccc;

  .progress {
    margin-top: 2px;
    margin-bottom: 5px;
  }

  .finished {
    display: none;
    position: relative;
    top: -1px;
    left: 2px;
    font-family: "Arial", sans-serif;
    font-size: 11px;
  }
`;

const StyledAlert = styled(Alert)`
  margin-top: 10px;
`;

class SystemJobsList extends React.Component {
  static propTypes = {
    jobs: PropTypes.arrayOf(PropTypes.object).isRequired,
  };

  _formatSystemJob = (job) => {
    return (
      <SystemJobWrap key={`job-${job.id}`}>
        <SystemJob job={job} />
      </SystemJobWrap>
    );
  };

  render() {
    const jobs = this.props.jobs.map(this._formatSystemJob);
    if (jobs.length === 0) {
      return (
        <StyledAlert bsStyle="info">
          <Icon name="info-circle" />{' '}
          &nbsp;No active system jobs.
        </StyledAlert>
      );
    }
    return (
      <span>
        {jobs}
      </span>
    );
  }
}

export default SystemJobsList;
