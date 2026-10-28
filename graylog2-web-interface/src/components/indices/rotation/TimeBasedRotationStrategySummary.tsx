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
import moment from 'moment';
import 'moment-duration-format';

type Props = {
  config: any;
};

const TimeBasedRotationStrategySummary = ({ config }: Props) => {
  const { t } = useTranslation('indexsets');
  const humanizedPeriod = () => {
    const duration = moment.duration(config.rotation_period);

    return `${duration.format()}, ${duration.humanize()}`;
  };

  return (
    <div>
      <dl>
        <dt>{t('strategies.rotation_summary_strategy')}</dt>
        <dd>{t('strategies.rotation_index_time')}</dd>
        <dt>{t('strategies.rotation_period_summary')}</dt>
        <dd>
          {config.rotation_period} ({humanizedPeriod()})
        </dd>
        <dt>{t('strategies.rotation_rotate_empty_summary')}</dt>
        <dd>{config.rotate_empty_index_set ? t('strategies.yes') : t('strategies.no')}</dd>
      </dl>
    </div>
  );
};

export default TimeBasedRotationStrategySummary;
