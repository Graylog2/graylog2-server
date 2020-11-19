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
// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { useField } from 'formik';

import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';
import { ButtonToolbar } from 'components/graylog';
import PropTypes from 'views/components/CustomPropTypes';

import timeRangeTypeMenuItems from './TimeRangeTypeMenuItems';

import { migrateTimeRangeToNewType } from '../TimerangeForForm';

type Props = {
  disabled: boolean,
};

export default function TimeRangeTypeSelector({ disabled }: Props) {
  const [{ value, onChange, name }] = useField('timerange');
  const { type: currentType } = value;
  const onSelect = useCallback((newType) => onChange({
    target: {
      value: migrateTimeRangeToNewType(value, newType),
      name,
    },
  }), [onChange, value]);

  return (
    <ButtonToolbar className="pull-left">
      <TimeRangeDropdownButton disabled={disabled} onSelect={onSelect}>
        {timeRangeTypeMenuItems(currentType)}
      </TimeRangeDropdownButton>
    </ButtonToolbar>
  );
}

TimeRangeTypeSelector.propTypes = {
  disabled: PropTypes.bool,
};

TimeRangeTypeSelector.defaultProps = {
  disabled: false,
};
