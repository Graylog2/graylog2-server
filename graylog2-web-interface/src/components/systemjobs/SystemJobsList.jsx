import PropTypes from 'prop-types';
import React from 'react';

import { Alert, Icon } from 'components/graylog';
import { SystemJob } from 'components/systemjobs';

class SystemJobsList extends React.Component {
  static propTypes = {
    jobs: PropTypes.arrayOf(PropTypes.object).isRequired,
  };

  _formatSystemJob = (job) => {
    return (
      <div className="systemjob systemjob-progress systemjob-active" key={`job-${job.id}`}>
        <SystemJob job={job} />
      </div>
    );
  };

  render() {
    const jobs = this.props.jobs.map(this._formatSystemJob);
    if (jobs.length === 0) {
      return (
        <Alert bsStyle="info" className="systemjobs-none">
          <Icon name="info-circle" />{' '}
          &nbsp;No active system jobs.
        </Alert>
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
