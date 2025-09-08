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
import React, { useState, useEffect } from 'react';
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
    const maxRotationPeriodErrorMessage = max_rotation_period
      ? ` and max ${moment.duration(max_rotation_period).humanize()}`
      : '';

    return isValidPeriod()
      ? moment.duration(rotationPeriod).humanize()
      : `invalid (min 1 hour${maxRotationPeriodErrorMessage})`;
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

  useEffect(() => {
    console.log(config);
  }, [config]);

  return (
    <div>
      <Input
        disabled={disabled}
        id="rotation-period"
        type="text"
        label="Rotation period (ISO8601 Duration)"
        value={rotationPeriod}
        onChange={handlePeriodUpdate}
        help={`How long an index gets written to before it is rotated. (i.e. "P1D" for 1 day, "PT6H" for 6 hours).${
          max_rotation_period
            ? ` The max rotation period is set to ${moment.duration(max_rotation_period).humanize()} by Administrator.`
            : ''
        }`}
        addonAfter={formatDuration()}
        bsStyle={validationState()}
        required
      />
      <Input
        disabled={disabled}
        id="rotate-empty-index-sets-checkbox"
        type="checkbox"
        label="Rotate empty index set"
        onChange={handleRotateEmptyIndexSetUpdate}
        checked={rotateEmptyIndexSet}
        help="Apply the rotation strategy even when the index set is empty (not recommended)."
      />
    </div>
  );
};

export default TimeBasedRotationStrategyConfiguration;
