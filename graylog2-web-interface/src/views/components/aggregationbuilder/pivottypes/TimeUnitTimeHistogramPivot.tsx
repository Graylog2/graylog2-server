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

import { DropdownButton, FormControl, HelpBlock, InputGroup, MenuItem } from 'components/graylog';
import FormsUtils from 'util/FormsUtils';
import { TimeUnits } from 'views/Constants';

import type { Interval, TimeUnitInterval } from './Interval';
import styles from './TimeUnitTimeHistogramPivot.css';

type OnChange = (Interval) => void;

type Props = {
  interval: TimeUnitInterval,
  onChange: OnChange,
};

const _changeValue = (event: SyntheticInputEvent<HTMLInputElement>, interval: TimeUnitInterval, onChange: OnChange) => {
  const value = FormsUtils.getValueFromInput(event.target);

  onChange({ ...interval, value });
};

const _changeUnit = (unit: string, interval: TimeUnitInterval, onChange: OnChange) => {
  onChange({ ...interval, unit });
};

const TimeUnitTimeHistogramPivot = ({ interval, onChange }: Props) => (
  <>
    <InputGroup className={styles.useFullWidth}>
      <FormControl type="number"
                   value={interval.value}
                   step="1"
                   min="1"
                   onChange={(e) => _changeValue(e, interval, onChange)} />
      <DropdownButton componentClass={InputGroup.Button}
                      id="input-dropdown-addon"
                      title={TimeUnits[interval.unit] || ''}
                      onChange={(newUnit) => _changeUnit(newUnit, interval, onChange)}>
        {Object.keys(TimeUnits).map((unit) => <MenuItem key={unit} onSelect={() => _changeUnit(unit, interval, onChange)}>{TimeUnits[unit]}</MenuItem>)}
      </DropdownButton>
    </InputGroup>
    <HelpBlock>The size of the buckets for this timestamp type.</HelpBlock>
  </>
);

export default TimeUnitTimeHistogramPivot;
