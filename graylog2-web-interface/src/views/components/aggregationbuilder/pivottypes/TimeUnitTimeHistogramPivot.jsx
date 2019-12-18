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
  <React.Fragment>
    <InputGroup className={styles.useFullWidth}>
      <FormControl type="number"
                   value={interval.value}
                   onChange={e => _changeValue(e, interval, onChange)} />
      <DropdownButton componentClass={InputGroup.Button}
                      id="input-dropdown-addon"
                      title={TimeUnits[interval.unit] || ''}
                      onChange={newUnit => _changeUnit(newUnit, interval, onChange)}>
        {Object.keys(TimeUnits).map(unit => <MenuItem key={unit} onSelect={() => _changeUnit(unit, interval, onChange)}>{TimeUnits[unit]}</MenuItem>)}
      </DropdownButton>
    </InputGroup>
    <HelpBlock>The size of the buckets for this timestamp type.</HelpBlock>
  </React.Fragment>
);

export default TimeUnitTimeHistogramPivot;
