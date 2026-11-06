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
import styled, { css } from 'styled-components';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { ClusterJobs, SystemJobs } from '@graylog/server-api';

import { ProgressBar, LinkToNode, RelativeTime, Icon } from 'components/common';
import { Button, Badge } from 'components/bootstrap';
import type { BadgeColor } from 'components/bootstrap/Badge';
import UserNotification from 'util/UserNotification';
import { SYSTEM_JOBS_QUERY_KEY } from 'components/systemjobs/useSystemJobs';
import translateApiError from 'util/translateApiError';
import useServerText from 'util/serverText';

enum JobStatus {
  Cancelled = 'cancelled',
  Complete = 'complete',
  Error = 'error',
  Paused = 'paused',
  Runnable = 'runnable',
  Running = 'running',
}

const BADGE_COLOR_BY_STATUS: Record<string, BadgeColor> = {
  cancelled: 'warning',
  complete: 'success',
  error: 'danger',
  queued: 'gray',
  running: 'primary',
};

const StyledProgressBar = styled(ProgressBar)`
  margin-top: 2px;
  margin-bottom: 5px;
`;

const JobWrap = styled.div`
  margin-bottom: 5px;
`;

const AcknowledgeButton = styled(Button)(
  ({ theme }) => css`
    && {
      color: ${theme.colors.text.primary};

      &:hover {
        color: ${theme.colors.variant.default};
      }
    }
  `,
);

type SystemJobProps = {
  job: {
    info?: string;
    id?: string;
    percent_complete?: number;
    is_cancelable?: boolean;
    name?: string;
    node_id?: string;
    started_at?: string;
    execution_duration?: string;
    job_status?: string;
    provides_progress?: boolean;
  };
};

const SystemJob = ({ job }: SystemJobProps) => {
  const { t } = useTranslation('system');
  // The server describes the jobs in English, e.g. "Archived documents in indices: graylog_1".
  const serverText = useServerText();
  const queryClient = useQueryClient();
  const jobIsOver =
    job.job_status === JobStatus.Complete ||
    job.percent_complete === 100 ||
    job.job_status === JobStatus.Cancelled ||
    job.job_status === JobStatus.Error;
  const mappedJobStatus = job.job_status === JobStatus.Runnable ? 'queued' : job.job_status;

  const _onAcknowledge = () => (e) => {
    e.preventDefault();

    SystemJobs.acknowledgeJob(job.id)
      .then(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_JOBS_QUERY_KEY });
      })
      .catch((error) => {
        UserNotification.error(translateApiError(error) ?? error.responseMessage, t('system_job.acknowledge_failed'));
      });
  };

  const _onCancel = () => (e) => {
    e.preventDefault();

    // eslint-disable-next-line no-alert
    if (window.confirm(t('system_job.cancel_confirm', { info: serverText(job.info) }))) {
      ClusterJobs.cancelJob(job.id)
        .then(() => {
          queryClient.invalidateQueries({ queryKey: SYSTEM_JOBS_QUERY_KEY });
        })
        .catch((error) => {
          UserNotification.error(translateApiError(error) ?? error.responseMessage, t('system_job.cancel_failed'));
        });
    }
  };

  return (
    <div>
      <JobWrap>
        <Icon name="settings" />{' '}
        <span data-toggle="tooltip" title={job.name}>
          {serverText(job.info)}
        </span>{' '}
        {t('system_job.on_node')} <LinkToNode nodeId={job.node_id} /> <RelativeTime dateTime={job.started_at} />{' '}
        <span data-toggle="tooltip" title={t('system_job.runtime', { duration: job.execution_duration })}>
          <Badge color={BADGE_COLOR_BY_STATUS[mappedJobStatus] ?? 'gray'} variant="light" dot style={{ marginLeft: 4 }}>
            {mappedJobStatus}
          </Badge>
        </span>
        {!jobIsOver && job.is_cancelable ? (
          <Button type="button" bsSize="xs" bsStyle="primary" className="pull-right" onClick={_onCancel()}>
            {t('system_job.cancel')}
          </Button>
        ) : (
          <AcknowledgeButton
            type="button"
            bsStyle="link"
            onClick={_onAcknowledge()}
            bsSize="xs"
            className="pull-right"
            title={t('system_job.acknowledge')}>
            <Icon name="close" />
          </AcknowledgeButton>
        )}
      </JobWrap>

      {!jobIsOver && job.provides_progress && (
        <StyledProgressBar bars={[{ value: job.percent_complete, bsStyle: 'info', animated: true }]} />
      )}
    </div>
  );
};

export default SystemJob;
