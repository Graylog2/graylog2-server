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
import { useState, useCallback } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';

import { getValueFromInput } from 'util/FormsUtils';
import { Input } from 'components/bootstrap';
import useMaxIndexRotationLimit from 'hooks/useMaxIndexRotationLimit';

export type TimeBasedSizeOptimizingStrategyConfig = {
  index_lifetime_hard: string,
  index_lifetime_soft: string,
  type: string,
}

type Props = {
  config: TimeBasedSizeOptimizingStrategyConfig,
  updateConfig: (config: Omit<TimeBasedSizeOptimizingStrategyConfig, 'type'>) => void,
}

export const durationToRoundedDays = (duration: string) => {
  return Math.round(moment.duration(duration).asDays());
};

const _validationLimit = (durationInDays, rotationLimit) => {
  return durationInDays <= durationToRoundedDays(rotationLimit);
};

const TimeBasedSizeOptimizingStrategy = ({ config: { index_lifetime_hard, index_lifetime_soft }, updateConfig }: Props) => {
  const [indexLifetimeHard, setIndexLifetimeHard] = useState(index_lifetime_hard);
  const [indexLifetimeSoft, setIndexLifetimeSoft] = useState(index_lifetime_soft);
  const softLifetimeAsDays = durationToRoundedDays(indexLifetimeSoft);
  const hardLifetimeAsDays = durationToRoundedDays(indexLifetimeHard);
  const maxRotationPeriod = useMaxIndexRotationLimit();
  const isMinGreaterThanMax = softLifetimeAsDays > hardLifetimeAsDays;

  const _isValidPeriod = useCallback((duration: string) => {
    const checkInDays = durationToRoundedDays(duration);

    return checkInDays >= 1 && (
      maxRotationPeriod ? _validationLimit(checkInDays, maxRotationPeriod) : true
    );
  }, [maxRotationPeriod]);

  const formatDuration = useCallback((duration: string) => {
    const maxRotationPeriodErrorMessage = maxRotationPeriod ? ` and max ${durationToRoundedDays(maxRotationPeriod)} days` : '';

    return _isValidPeriod(duration) ? `${durationToRoundedDays(duration)} days` : `invalid (min 1 day${maxRotationPeriodErrorMessage})`;
  }, [_isValidPeriod, maxRotationPeriod]);

  const getHardlifetimeHelp = () => {
    return isMinGreaterThanMax
      ? `Min lifetime ${softLifetimeAsDays} cannot be greater than the Maximum ${hardLifetimeAsDays}`
      : 'The maximum time data in the is index kept before it is rotated. At least 1 day more than the minimum above. (i.e. "P1D" for 1 day).';
  };

  const onLifetimeSoftChange = (event: React.ChangeEvent<HTMLOptionElement>): void => {
    const inputValue = getValueFromInput(event.target);

    setIndexLifetimeSoft(inputValue);

    if (_isValidPeriod(inputValue)) {
      updateConfig({ index_lifetime_hard: indexLifetimeHard, index_lifetime_soft: inputValue });
    }
  };

  const onLifetimeHardChange = (event: React.ChangeEvent<HTMLOptionElement>): void => {
    const inputValue = getValueFromInput(event.target);

    setIndexLifetimeHard(inputValue);

    if (_isValidPeriod(inputValue)) {
      updateConfig({ index_lifetime_soft: indexLifetimeSoft, index_lifetime_hard: inputValue });
    }
  };

  const validationState = (duration: string) => {
    if (_isValidPeriod(duration) && !isMinGreaterThanMax) {
      return null;
    }

    return 'error';
  };

  const maxRotationPeriodHelpText = maxRotationPeriod ? ` The max rotation period is set to ${durationToRoundedDays(maxRotationPeriod)} by Administrator.` : '';

  return (
    <div>
      <Input id="rotation-index-lifetime-soft"
             type="text"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             label="Minimum lifetime (ISO8601 Duration)"
             onChange={onLifetimeSoftChange}
             value={indexLifetimeSoft}
             help={`The minimum time data in the is index kept before it is rotated. (i.e. "P1D" for 1 day).${maxRotationPeriodHelpText}`}
             addonAfter={formatDuration(indexLifetimeSoft)}
             bsStyle={validationState(indexLifetimeSoft)}
             required />
      <Input id="rotation-index-lifetime-hard"
             type="text"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             label="Maximum lifetime (ISO8601 Duration)"
             onChange={onLifetimeHardChange}
             value={indexLifetimeHard}
             help={`${getHardlifetimeHelp()} ${maxRotationPeriodHelpText}`}
             addonAfter={formatDuration(indexLifetimeHard)}
             bsStyle={validationState(indexLifetimeHard)}
             required />
    </div>
  );
};

TimeBasedSizeOptimizingStrategy.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default TimeBasedSizeOptimizingStrategy;
