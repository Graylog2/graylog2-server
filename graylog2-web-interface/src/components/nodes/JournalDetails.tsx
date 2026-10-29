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
import * as React from 'react';
import numeral from 'numeral';
import moment from 'moment';
import 'moment-duration-format';
import styled from 'styled-components';
import { useQuery } from '@tanstack/react-query';
import { Trans, useTranslation } from 'react-i18next';

import { ClusterJournal } from '@graylog/server-api';

import { Link, Spinner, RelativeTime } from 'components/common';
import { Row, Col, Alert } from 'components/bootstrap';
import ProgressBar, { Bar } from 'components/common/ProgressBar';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import NumberUtils from 'util/NumberUtils';
import Routes from 'routing/Routes';
import { defaultOnError } from 'util/conditional/onError';
import { useNodeMetrics } from 'hooks/useMetrics';

const JournalUsageProgressBar = styled(ProgressBar)`
  margin-bottom: 5px;
  margin-top: 10px;

  ${Bar} {
    min-width: 3em;
  }
`;

type Props = {
  nodeId: string;
};
const metricNames = {
  append: 'org.graylog2.journal.append.1-sec-rate',
  read: 'org.graylog2.journal.read.1-sec-rate',
  segments: 'org.graylog2.journal.segments',
  entriesUncommitted: 'org.graylog2.journal.entries-uncommitted',
  utilizationRatio: 'org.graylog2.journal.utilization-ratio',
  oldestSegment: 'org.graylog2.journal.oldest-segment',
};

const metricValues = Object.values(metricNames);

const fetchJournalInfo = (nodeId: string) =>
  defaultOnError(ClusterJournal.get(nodeId), 'Could not get journal information');

const JournalDetails = ({ nodeId }: Props) => {
  const { t } = useTranslation('system');
  const { data: journalInformation } = useQuery({
    queryKey: ['journal', 'info', nodeId],
    queryFn: () => fetchJournalInfo(nodeId),
  });

  const { data: nodeMetrics, isLoading: metricsLoading } = useNodeMetrics(nodeId, metricValues);

  const _isLoading = metricsLoading || !journalInformation;

  if (_isLoading) {
    return <Spinner text={t('journal_details.loading')} />;
  }

  if (!journalInformation.enabled) {
    return <Alert bsStyle="warning">{t('journal_details.disabled')}</Alert>;
  }

  const metrics = journalInformation.enabled ? MetricsExtractor.getValuesForNode(nodeMetrics, metricNames) : {};

  if (Object.keys(metrics).length === 0) {
    return <Alert bsStyle="warning">{t('journal_details.unavailable')}</Alert>;
  }

  const oldestSegment = moment(metrics.oldestSegment);
  let overcommittedWarning;

  if (metrics.utilizationRatio >= 1) {
    overcommittedWarning = (
      <span>
        <Trans
          t={t}
          i18nKey="journal_details.overcommitted"
          components={{ strong: <strong />, overviewLink: <Link to={Routes.SYSTEM.OVERVIEW} /> }}
        />
        <br />
      </span>
    );
  }

  return (
    <Row className="row-sm">
      <Col md={6}>
        <h3>{t('journal_details.configuration')}</h3>
        <dl className="system-journal">
          <dt>{t('journal_details.path')}</dt>
          <dd>{journalInformation.journal_config.directory}</dd>
          <dt>{t('journal_details.earliest_entry')}</dt>
          <dd>
            <RelativeTime dateTime={oldestSegment} />
          </dd>
          <dt>{t('journal_details.maximum_size')}</dt>
          <dd>{NumberUtils.formatBytes(journalInformation.journal_config.max_size)}</dd>
          <dt>{t('journal_details.maximum_age')}</dt>
          <dd>{moment.duration(journalInformation.journal_config.max_age).format('d [days] h [hours] m [minutes]')}</dd>
          <dt>{t('journal_details.flush_policy')}</dt>
          <dd>
            {t('journal_details.flush_policy_value', {
              messages: numeral(journalInformation.journal_config.flush_interval).format('0,0'),
              duration: moment
                .duration(journalInformation.journal_config.flush_age)
                .format('h [hours] m [minutes] s [seconds]'),
            })}
          </dd>
        </dl>
      </Col>
      <Col md={6}>
        <h3>{t('journal_details.utilization')}</h3>
        <JournalUsageProgressBar
          bars={[
            {
              value: metrics.utilizationRatio * 100,
              label: NumberUtils.formatPercentage(metrics.utilizationRatio),
            },
          ]}
        />
        {overcommittedWarning}
        <Trans
          t={t}
          i18nKey="journal_details.unprocessed_messages"
          values={{ messages: numeral(metrics.entriesUncommitted).format('0,0'), segments: metrics.segments }}
          components={{ strong: <strong /> }}
        />
        <br />
        <Trans
          t={t}
          i18nKey="journal_details.appended_and_read"
          values={{ appended: numeral(metrics.append).format('0,0'), read: numeral(metrics.read).format('0,0') }}
          components={{ strong: <strong /> }}
        />
      </Col>
    </Row>
  );
};

export default JournalDetails;
