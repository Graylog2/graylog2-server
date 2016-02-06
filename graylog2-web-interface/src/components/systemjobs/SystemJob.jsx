import React from 'react';
import { ProgressBar } from 'react-bootstrap';

import { LinkToNode, Timestamp } from 'components/common';

const SystemJob = React.createClass({
  render() {
    const job = this.props.job;
    const progress = job.percent_complete < 100 ?
      (<ProgressBar bsStyle="info" active now={job.percent_complete}/>) : <span className="label label-success finished">Finished!</span>;
    return (
      <div>
        <i className="fa fa-cog" />{' '}
        <span data-toggle="tooltip" title={job.name}>{job.info}</span>{' '}
        - Started on <LinkToNode nodeId={job.node_id}/>{' '}
        <Timestamp dateTime={job.started_at} relative/>{' '}

        {progress}
      </div>
    );
  },
});

export default SystemJob;
