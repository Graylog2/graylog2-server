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
import { useContext } from 'react';
import styled from 'styled-components';
import { Field, useFormikContext } from 'formik';

import { TimeUnits } from 'views/Constants';
import { Icon, FormikFormGroup } from 'components/common';
import { FormControl, Checkbox, DropdownButton, HelpBlock, MenuItem, InputGroup } from 'components/graylog';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import { Input } from 'components/bootstrap';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

import FieldSelect from './FieldSelect';

const Wrapper = styled.div``;

const RangeSelect = styled.div`
  display: flex;
  align-items: center;
`;

const DirectionOptions = styled.div`
  display: flex;

  div:first-child {
    margin-right: 5px;
  }
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
  index: number,
}

const GroupBy = ({ index }: Props) => {
  const { values: { groupBy }, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const fieldType = groupBy.groupings[index].field.type;
  const fieldTypes = useContext(FieldTypesContext);

  const toggleIntervalType = (name, currentType, onChange) => {
    if (currentType === 'auto') {
      onChange({ target: { name, value: { type: 'timeunit', value: 1, unit: 'minutes' } } });
    } else {
      onChange({ target: { name, value: { type: 'auto', scaling: 1.0 } } });
    }
  };

  const onChangeField = (e, name, onChange) => {
    const fieldName = e.target.value;
    const newField = fieldTypes.all.find((field) => field.name === fieldName);
    const newFieldType = newField?.type.type === 'date' ? 'time' : 'values';

    if (fieldType !== newFieldType) {
      if (newFieldType === 'time') {
        setFieldValue(`groupBy.groupings.${index}.interval`, {
          type: 'auto',
          scaling: 1.0,
        });
      }

      if (newFieldType === 'values') {
        setFieldValue(`groupBy.groupings.${index}.limit`, 15);
      }
    }

    onChange({
      target: {
        name,
        value: {
          field: newField.name,
          type: newFieldType,
        },
      },
    });
  };

  return (
    <Wrapper>
      <Field name={`groupBy.groupings.${index}.direction`}>
        {({ field: { name, value, onChange, onBlur }, meta: { error } }) => (
          <Input id="group-by-direction"
                 label="Direction"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <DirectionOptions>
              <Input defaultChecked={value === 'row'}
                     formGroupClassName=""
                     id={name}
                     label="Row"
                     onBlur={onBlur}
                     onChange={onChange}
                     type="radio"
                     value="row" />
              <Input defaultChecked={value === 'column'}
                     formGroupClassName=""
                     id={name}
                     label="Column"
                     onBlur={onBlur}
                     onChange={onChange}
                     type="radio"
                     value="column" />
            </DirectionOptions>
          </Input>
        )}
      </Field>

      <Field name={`groupBy.groupings.${index}.field`}>
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <FieldSelect id="group-by-field-select"
                       label="Field"
                       onChange={(e) => onChangeField(e, name, onChange)}
                       error={error}
                       clearable={false}
                       ariaLabel="Field"
                       name={name}
                       value={value.field}
                       aria-label="Select a field" />
        )}
      </Field>

      {fieldType === 'time' && (
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
      )}

      {fieldType === 'values' && (
        <FormikFormGroup label="Limit" name={`groupBy.groupings.${index}.limit`} type="number" />
      )}
    </Wrapper>
  );
};

export default GroupBy;
