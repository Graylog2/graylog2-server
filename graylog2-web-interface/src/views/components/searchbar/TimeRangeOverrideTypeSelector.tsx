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

import { MenuItem, ButtonToolbar } from 'components/graylog';
import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';

import timeRangeTypeMenuItems from './TimeRangeTypeMenuItems';

import { migrateTimeRangeToNewType } from '../TimerangeForForm';

const TimeRangeOverrideTypeSelector = () => {
  const [{ value, onChange, name }] = useField('timerange');
  const { type } = value || {};
  const onSelect = useCallback((newType) => onChange({
    target: {
      value: migrateTimeRangeToNewType(value, newType),
      name,
    },
  }), [onChange, value]);

  return (
    <ButtonToolbar className="pull-left">
      <TimeRangeDropdownButton onSelect={onSelect}>
        <MenuItem eventKey="disabled"
                  active={type === undefined}>
          No Override
        </MenuItem>
        {timeRangeTypeMenuItems(type)}
      </TimeRangeDropdownButton>
    </ButtonToolbar>
  );
};

export default TimeRangeOverrideTypeSelector;
