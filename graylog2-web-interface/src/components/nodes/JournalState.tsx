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
import numeral from 'numeral';
import { Trans, useTranslation } from 'react-i18next';

import { Spinner } from 'components/common';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import { useNodeMetrics } from 'hooks/useMetrics';

type Props = {
  nodeId: string;
};
const metricNames = {
  append: 'org.graylog2.journal.append.1-sec-rate',
  read: 'org.graylog2.journal.read.1-sec-rate',
  segments: 'org.graylog2.journal.segments',
  entriesUncommitted: 'org.graylog2.journal.entries-uncommitted',
};

const metricValues = Object.values(metricNames);

const JournalState = ({ nodeId }: Props) => {
  const { t } = useTranslation('system');
  const { data: nodeMetrics, isLoading } = useNodeMetrics(nodeId, metricValues);
  const _isLoading = isLoading || !nodeMetrics;

  if (_isLoading) {
    return <Spinner text={t('journal_state.loading')} />;
  }

  const _metrics = MetricsExtractor.getValuesForNode(nodeMetrics, metricNames);

  if (Object.keys(_metrics).length === 0) {
    return <span>{t('journal_state.unavailable')}</span>;
  }

  return (
    <span>
      <Trans
        t={t}
        i18nKey="journal_state.summary"
        count={_metrics.segments}
        values={{
          messages: numeral(_metrics.entriesUncommitted).format('0,0'),
          appended: numeral(_metrics.append).format('0,0'),
          read: numeral(_metrics.read).format('0,0'),
        }}
        components={{ strong: <strong /> }}
      />
    </span>
  );
};

export default JournalState;
