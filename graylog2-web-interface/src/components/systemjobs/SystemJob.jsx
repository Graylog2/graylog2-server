/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Button, ProgressBar } from 'components/graylog';
import { LinkToNode, Timestamp, Icon } from 'components/common';
import ActionsProvider from 'injection/ActionsProvider';

const SystemJobsActions = ActionsProvider.getActions('SystemJobs');

const StyledProgressBar = styled(ProgressBar)`
  margin-top: 2px;
  margin-bottom: 5px;
`;

const JobWrap = styled.div`
  line-height: 1.5;
  margin-bottom: 5px;
`;

class SystemJob extends React.Component {
  static propTypes = {
    job: PropTypes.arrayOf(PropTypes.shape({
      info: PropTypes.string,
      id: PropTypes.string,
      percent_complete: PropTypes.number,
      is_cancelable: PropTypes.bool,
      name: PropTypes.string,
      node_id: PropTypes.string,
      started_at: PropTypes.string,
    })).isRequired,
  }

  _onCancel = (job) => {
    return (e) => {
      e.preventDefault();

      // eslint-disable-next-line no-alert
      if (window.confirm(`Are you sure you want to cancel system job "${job.info}"?`)) {
        SystemJobsActions.cancelJob(job.id);
      }
    };
  };

  render() {
    const { job } = this.props;
    const progress = job.percent_complete < 100
      ? <StyledProgressBar bars={[{ value: job.percent_complete, bsStyle: 'info', animated: true }]} />
      : <span className="label label-success finished">Finished!</span>;
    const cancel = job.is_cancelable
      ? (<Button type="button" bsSize="xs" bsStyle="primary" className="pull-right" onClick={this._onCancel(job)}>Cancel Job</Button>) : null;

    return (
      <div>
        <JobWrap>
          <Icon name="cog" />{' '}
          <span data-toggle="tooltip" title={job.name}>{job.info}</span>{' '}
          - Started on <LinkToNode nodeId={job.node_id} />{' '}
          <Timestamp dateTime={job.started_at} relative />{' '}
          {cancel}
        </JobWrap>

        {progress}
      </div>
    );
  }
}

export default SystemJob;
