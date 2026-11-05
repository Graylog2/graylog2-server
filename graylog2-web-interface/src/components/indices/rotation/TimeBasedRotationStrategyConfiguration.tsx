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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import moment from 'moment/moment';

import type { IndexRotationConfigComponentProps } from 'components/indices/rotation/types';
import { Input } from 'components/bootstrap';

type Config = {
  rotation_period: string;
  max_rotation_period: string;
  rotate_empty_index_set: boolean;
};

const TimeBasedRotationStrategyConfiguration: React.FC<IndexRotationConfigComponentProps<Config>> = ({
  config,
  updateConfig,
  disabled = false,
}) => {
  const { t } = useTranslation('indexsets');
  const { max_rotation_period } = config;

  const [rotationPeriod, setRotationPeriod] = useState<string>(config.rotation_period);
  const [rotateEmptyIndexSet, setRotateEmptyIndexSet] = useState<boolean>(config.rotate_empty_index_set);

  const validationLimit = (durationInMilliseconds: number) =>
    durationInMilliseconds <= moment.duration(max_rotation_period).asMilliseconds();

  const isValidPeriod = (duration?: string) => {
    const check = duration || rotationPeriod;
    const checkInMilliseconds = moment.duration(check).asMilliseconds();

    return checkInMilliseconds >= 3600000 && (max_rotation_period ? validationLimit(checkInMilliseconds) : true);
  };

  const validationState = () => {
    if (isValidPeriod()) {
      return undefined;
    }

    return 'error' as const;
  };

  const formatDuration = () => {
    if (isValidPeriod()) {
      return moment.duration(rotationPeriod).humanize();
    }

    return max_rotation_period
      ? t('strategies.rotation_period_invalid_max', { max: moment.duration(max_rotation_period).humanize() })
      : t('strategies.rotation_period_invalid');
  };

  const handlePeriodUpdate = (e) => {
    let period = e.target.value.toUpperCase();

    if (!period.startsWith('P')) period = `P${period}`;

    setRotationPeriod(period);

    if (isValidPeriod(period)) {
      updateConfig({
        ...config,
        rotation_period: period,
      });
    }
  };

  const handleRotateEmptyIndexSetUpdate = (e) => {
    setRotateEmptyIndexSet(e.target.checked);

    updateConfig({
      ...config,
      rotate_empty_index_set: e.target.checked,
    });
  };

  return (
    <div>
      <Input
        disabled={disabled}
        id="rotation-period"
        type="text"
        label={t('strategies.rotation_period_label')}
        value={rotationPeriod}
        onChange={handlePeriodUpdate}
        help={[
          t('strategies.rotation_period_help'),
          max_rotation_period &&
            t('strategies.rotation_period_max_help', { max: moment.duration(max_rotation_period).humanize() }),
        ]
          .filter(Boolean)
          .join(' ')}
        addonAfter={formatDuration()}
        bsStyle={validationState()}
        required
      />
      <Input
        disabled={disabled}
        id="rotate-empty-index-sets-checkbox"
        type="checkbox"
        label={t('strategies.rotation_rotate_empty_label')}
        onChange={handleRotateEmptyIndexSetUpdate}
        checked={rotateEmptyIndexSet}
        help={t('strategies.rotation_rotate_empty_help')}
      />
    </div>
  );
};

export default TimeBasedRotationStrategyConfiguration;
