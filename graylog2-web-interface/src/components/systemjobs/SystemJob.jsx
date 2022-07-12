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
import styled, { css } from 'styled-components';

import { ProgressBar, LinkToNode, RelativeTime, Icon } from 'components/common';
import { Button } from 'components/bootstrap';
import { SystemJobsActions } from 'stores/systemjobs/SystemJobsStore';
import UserNotification from 'util/UserNotification';
import { StyledBadge } from 'components/bootstrap/Badge';

const StatusBadge = styled(StyledBadge)(({ status, theme }) => {
  const { success, info, warning, danger } = theme.colors.variant.dark;
  const statuses = {
    complete: success,
    runnable: info,
    running: info,
    edited: warning,
    error: danger,
    cancelled: warning,
  };

  return css`
    margin-left: 4px;
    background-color: ${statuses[status]};
    color: ${theme.utils.readableColor(statuses[status])};
  `;
});

const StyledProgressBar = styled(ProgressBar)`
  margin-top: 2px;
  margin-bottom: 5px;
`;

const JobWrap = styled.div`
  line-height: 1.5;
  margin-bottom: 5px;
`;

const SystemJob = ({ job }) => {
  const jobIsDone = job.job_status === 'complete' || job.percent_complete === 100;

  const _onAcknowledge = () => {
    return (e) => {
      e.preventDefault();

      SystemJobsActions.acknowledgeJob(job.id).catch((error) => {
        UserNotification.error(error.responseMessage, 'Unable to acknowledge the job');
      });
    };
  };

  const _onCancel = () => {
    return (e) => {
      e.preventDefault();

      // eslint-disable-next-line no-alert
      if (window.confirm(`Are you sure you want to cancel system job "${job.info}"?`)) {
        SystemJobsActions.cancelJob(job.id).catch((error) => {
          UserNotification.error(error.responseMessage, 'Unable to cancel the job');
        });
      }
    };
  };

  return (
    <div>
      <JobWrap>
        <Icon name="cog" />{' '}
        <span data-toggle="tooltip" title={job.name}>{job.info}</span>{' '}
        - Started on <LinkToNode nodeId={job.node_id} />{' '}
        <RelativeTime dateTime={job.started_at} />{' '}
        -&nbsp;
        <StatusBadge status={job.job_status}>{job.job_status}</StatusBadge>&nbsp;
        {!jobIsDone && job.is_cancelable
          ? (<Button type="button" bsSize="xs" bsStyle="primary" className="pull-right" onClick={_onCancel()}>Cancel</Button>)
          : (<Button type="button" bsSize="xs" bsStyle="success" className="pull-right" onClick={_onAcknowledge()}>Acknowledge</Button>)}
      </JobWrap>

      {!jobIsDone && <StyledProgressBar bars={[{ value: job.percent_complete, bsStyle: 'info', animated: true }]} />}
    </div>
  );
};

SystemJob.propTypes = {
  job: PropTypes.shape({
    info: PropTypes.string,
    id: PropTypes.string,
    percent_complete: PropTypes.number,
    is_cancelable: PropTypes.bool,
    name: PropTypes.string,
    node_id: PropTypes.string,
    started_at: PropTypes.string,
    job_status: PropTypes.string,
  }).isRequired,
};

export default SystemJob;
