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
import styled from 'styled-components';
import { Field } from 'formik';

import { Icon } from 'components/common';
import { TimeUnits } from 'views/Constants';
import { FormControl, Checkbox, DropdownButton, HelpBlock, MenuItem, InputGroup } from 'components/graylog';
import { Input } from 'components/bootstrap';

const RangeSelect = styled.div`
  display: flex;
  align-items: center;
`;

const CurrentScale = styled.div`
  min-width: 30px;
  margin-left: 5px;
  text-align: right;
`;

const TypeCheckboxWrapper = styled.div`
  margin-bottom: 5px;
`;

type Props = {
  index: number
};

const Time = ({ index }: Props) => {
  const toggleIntervalType = (name, currentType, onChange) => {
    if (currentType === 'auto') {
      onChange({ target: { name, value: { type: 'timeunit', value: 1, unit: 'minutes' } } });
    } else {
      onChange({ target: { name, value: { type: 'auto', scaling: 1.0 } } });
    }
  };

  return (
    <Field name={`groupBy.groupings.${index}.interval`}>
      {({ field: { name, value, onChange }, meta: { error } }) => (
        <Input id="group-by-interval"
               label="Interval"
               error={error}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <TypeCheckboxWrapper>
            <Checkbox onChange={() => toggleIntervalType(name, value.type, onChange)}
                      checked={value.type === 'auto'}>
              Auto
            </Checkbox>
          </TypeCheckboxWrapper>

          {value.type === 'auto' && (
            <>
              <RangeSelect>
                <Icon name="search-minus" size="lg" style={{ paddingRight: '0.5rem' }} />
                <FormControl type="range"
                             style={{ padding: 0, border: 0 }}
                             min={0.5}
                             max={10}
                             step={0.5}
                             value={value.scaling ? (1 / value.scaling) : 1.0}
                             onChange={(e) => onChange({ target: { name, value: { ...value, scaling: 1 / parseFloat(e.target.value) } } })} />
                <Icon name="search-plus" size="lg" style={{ paddingLeft: '0.5rem' }} />
                <CurrentScale>
                  {value.scaling ? (1 / value.scaling) : 1.0}x
                </CurrentScale>
              </RangeSelect>
              <HelpBlock className="no-bm">
                A smaller granularity leads to <strong>less</strong>, a bigger to <strong>more</strong> values.
              </HelpBlock>
            </>
          )}
          {value.type !== 'auto' && (
            <>
              <InputGroup>
                <FormControl type="number"
                             value={value.value}
                             step="1"
                             min="1"
                             onChange={(e) => onChange({ target: { name, value: { ...value, value: e.target.value } } })} />
                <InputGroup.Button>
                  <DropdownButton id="input-dropdown-addon"
                                  title={TimeUnits[value.unit] || ''}>
                    {Object.keys(TimeUnits).map((unit) => <MenuItem key={unit} onSelect={() => onChange({ target: { name, value: { ...value, unit } } })}>{TimeUnits[unit]}</MenuItem>)}
                  </DropdownButton>
                </InputGroup.Button>
              </InputGroup>
              <HelpBlock className="no-bm">
                The size of the buckets for this timestamp type.
              </HelpBlock>
            </>
          )}
        </Input>
      )}
    </Field>
  );
};

export default Time;
