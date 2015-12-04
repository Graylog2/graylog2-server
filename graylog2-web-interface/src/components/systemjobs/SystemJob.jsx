import React from 'react';
import moment from 'moment';
import { ProgressBar } from 'react-bootstrap';

import { LinkToNode } from 'components/common';

const SystemJob = React.createClass({
  render() {
    const job = this.props.job;
    const progress = job.percent_complete < 100 ?
      (<ProgressBar bsStyle="info" active now={job.percent_complete}>
      </ProgressBar>) : <span className="label label-success finished">Finished!</span>;
    return (
      <div>
        <i className="fa fa-cog" />{' '}
        Started <span data-toggle="tooltip" title={job.info}>{job.name}</span>{' '}
        on <LinkToNode nodeId={job.node_id}/>{' '}
        {moment(job.started_at).fromNow()}{' '}

        {progress}
      </div>
    );
  },
});

export default SystemJob;
