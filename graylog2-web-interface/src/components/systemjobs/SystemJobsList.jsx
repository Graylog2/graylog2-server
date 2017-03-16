import React from 'react';
import { Alert } from 'react-bootstrap';

import { SystemJob } from 'components/systemjobs';

const SystemJobsList = React.createClass({
  propTypes: {
    jobs: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
  },
  _formatSystemJob(job) {
    return (
      <div className="systemjob systemjob-progress systemjob-active" key={`job-${job.id}`}>
        <SystemJob job={job} />
      </div>
    );
  },
  render() {
    const jobs = this.props.jobs.map(this._formatSystemJob);
    if (jobs.length === 0) {
      return (
        <Alert bsStyle="info" className="systemjobs-none">
          <i className="fa fa-info-circle" />{' '}
          &nbsp;No active system jobs.
        </Alert>
      );
    }
    return (
      <span>
        {jobs}
      </span>
    );
  },
});

export default SystemJobsList;
