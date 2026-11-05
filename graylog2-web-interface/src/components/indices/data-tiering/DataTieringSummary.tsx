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
import { useTranslation } from 'react-i18next';

import type { DataTieringConfig } from 'components/indices/data-tiering';
import { durationToRoundedDays } from 'components/indices/data-tiering';

type Props = {
  config: DataTieringConfig;
};

const DataTieringSummary = ({ config }: Props) => {
  const { t } = useTranslation('indexsets');

  return (
    <div>
      <dl>
        <dt>{t('data_tiering.summary_rotation_strategy')}</dt>
        <dd>{t('data_tiering.summary_data_tiering')}</dd>
        <dt>{t('data_tiering.summary_max')}</dt>
        <dd>{t('data_tiering.days', { count: durationToRoundedDays(config.index_lifetime_max) })}</dd>
        <dt>{t('data_tiering.summary_min')}</dt>
        <dd>{t('data_tiering.days', { count: durationToRoundedDays(config.index_lifetime_min) })}</dd>
      </dl>
    </div>
  );
};

export default DataTieringSummary;
