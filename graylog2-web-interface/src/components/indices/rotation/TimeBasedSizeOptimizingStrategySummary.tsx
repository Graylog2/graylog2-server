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
import { useTranslation } from 'react-i18next';

import type { TimeBasedSizeOptimizingStrategyConfig } from './TimeBasedSizeOptimizingStrategyConfiguration';
import { durationToRoundedDays } from './TimeBasedSizeOptimizingStrategyConfiguration';

const TimeBasedSizeOptimizingStrategySummary = ({
  config: { index_lifetime_max, index_lifetime_min },
}: {
  config: TimeBasedSizeOptimizingStrategyConfig;
}) => {
  const { t } = useTranslation('indexsets');
  const minLifetimeAsDays = durationToRoundedDays(index_lifetime_min);
  const maxLifetimeAsDays = durationToRoundedDays(index_lifetime_max);

  return (
    <div>
      <dl>
        <dt>{t('strategies.rotation_summary_strategy')}</dt>
        <dd>{t('strategies.rotation_time_size_optimizing')}</dd>
        <dt>{t('strategies.rotation_min_lifetime')}</dt>
        <dd>
          {index_lifetime_min} ({t('data_tiering.days', { count: minLifetimeAsDays })})
        </dd>
        <dt>{t('strategies.rotation_max_lifetime')}</dt>
        <dd>
          {index_lifetime_max} ({t('data_tiering.days', { count: maxLifetimeAsDays })})
        </dd>
      </dl>
    </div>
  );
};

export default TimeBasedSizeOptimizingStrategySummary;
