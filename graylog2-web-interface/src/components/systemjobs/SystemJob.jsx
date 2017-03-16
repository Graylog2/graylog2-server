import React from 'react';
import { ProgressBar } from 'react-bootstrap';

import { LinkToNode, Timestamp } from 'components/common';

import ActionsProvider from 'injection/ActionsProvider';
const SystemJobsActions = ActionsProvider.getActions('SystemJobs');

const SystemJob = React.createClass({
  _onCancel(job) {
    return (e) => {
      e.preventDefault();
      if (window.confirm(`Are you sure you want to cancel system job "${job.info}"?`)) {
        SystemJobsActions.cancelJob(job.id);
      }
    };
  },
  render() {
    const job = this.props.job;
    const progress = job.percent_complete < 100 ?
      (<ProgressBar bsStyle="info" active now={job.percent_complete} />) : <span className="label label-success finished">Finished!</span>;
    const cancel = job.is_cancelable ?
      (<button type="button" className="btn btn-primary btn-xs pull-right" onClick={this._onCancel(job)}>Cancel Job</button>) : null;

    return (
      <div>
        <div style={{ lineHeight: 1.5, marginBottom: 5 }}>
          <i className="fa fa-cog" />{' '}
          <span data-toggle="tooltip" title={job.name}>{job.info}</span>{' '}
          - Started on <LinkToNode nodeId={job.node_id} />{' '}
          <Timestamp dateTime={job.started_at} relative />{' '}
          {cancel}
        </div>

        {progress}
      </div>
    );
  },
});

export default SystemJob;
