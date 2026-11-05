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
import { Trans, useTranslation } from 'react-i18next';
import styled from 'styled-components';
import { Field } from 'formik';

import { Icon, HoverForHelp } from 'components/common';
import { TimeUnits } from 'views/Constants';
import { FormControl, Checkbox, MenuItem, InputGroup, Input } from 'components/bootstrap';
import DropdownButton from 'components/bootstrap/DropdownButton';

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

const IntervalCheckboxDescWithHelp = styled.div`
  display: flex;
  align-items: center;
`;

const IntervalHoverForHelp = styled(HoverForHelp)`
  margin-left: 5px;
`;

type Props = {
  index: number;
};

const TIME_UNIT_LABELS = {
  seconds: 'wizard_units.time_units.seconds',
  minutes: 'wizard_units.time_units.minutes',
  hours: 'wizard_units.time_units.hours',
  days: 'wizard_units.time_units.days',
  weeks: 'wizard_units.time_units.weeks',
  months: 'wizard_units.time_units.months',
} as const satisfies Record<keyof typeof TimeUnits, string>;

const IntervalCheckboxDesc = () => {
  const { t } = useTranslation('aggregation');

  return (
    <IntervalCheckboxDescWithHelp>
      {t('wizard_grouping.auto')}
      <IntervalHoverForHelp title={t('wizard_grouping.interval_types')}>
        <ul>
          <li>
            <h4>{t('wizard_grouping.auto_bucket_size')}</h4>
            <Trans t={t} i18nKey="wizard_grouping.auto_bucket_size_help" components={{ strong: <strong /> }} />
          </li>

          <li>
            <h4>{t('wizard_grouping.fixed_bucket_size')}</h4>
            {t('wizard_grouping.fixed_bucket_size_help')}
          </li>
        </ul>
      </IntervalHoverForHelp>
    </IntervalCheckboxDescWithHelp>
  );
};

const toggleIntervalType = (name, currentType, onChange) => {
  if (currentType === 'auto') {
    onChange({ target: { name, value: { type: 'timeunit', value: 1, unit: 'minutes' } } });
  } else {
    onChange({ target: { name, value: { type: 'auto', scaling: 1.0 } } });
  }
};

const StyledFormControl = styled(FormControl)`
  padding: 0;
  border: 0;
`;

const Time = ({ index }: Props) => {
  const { t } = useTranslation('aggregation');

  return (
    <Field name={`groupBy.groupings.${index}.interval`}>
      {({ field: { name, value, onChange }, meta: { error } }) => (
        <Input
          id="group-by-interval"
          label={t('wizard_grouping.interval')}
          error={error}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9">
          <TypeCheckboxWrapper>
            <Checkbox onChange={() => toggleIntervalType(name, value.type, onChange)} checked={value.type === 'auto'}>
              <IntervalCheckboxDesc />
            </Checkbox>
          </TypeCheckboxWrapper>

          {value.type === 'auto' && (
            <RangeSelect>
              <Icon name="zoom_out" size="lg" style={{ paddingRight: '0.5rem' }} />
              <StyledFormControl
                type="range"
                min={0.5}
                max={10}
                step={0.5}
                value={value.scaling ? 1 / value.scaling : 1.0}
                onChange={(e) =>
                  onChange({
                    target: {
                      name,
                      value: { ...value, scaling: 1 / parseFloat((e.target as HTMLInputElement).value) },
                    },
                  })
                }
              />
              <Icon name="zoom_in" size="lg" style={{ paddingLeft: '0.5rem' }} />
              <CurrentScale>
                {t('wizard_grouping.scale', { scale: value.scaling ? 1 / value.scaling : 1.0 })}
              </CurrentScale>
            </RangeSelect>
          )}
          {value.type !== 'auto' && (
            <InputGroup>
              <FormControl
                type="number"
                value={value.value}
                step="1"
                min="1"
                onChange={(e) =>
                  onChange({ target: { name, value: { ...value, value: (e.target as HTMLInputElement).value } } })
                }
              />
              <InputGroup.Button>
                <DropdownButton title={TimeUnits[value.unit] ? t(TIME_UNIT_LABELS[value.unit]) : ''}>
                  {Object.keys(TimeUnits).map((unit) => (
                    <MenuItem key={unit} onSelect={() => onChange({ target: { name, value: { ...value, unit } } })}>
                      {t(TIME_UNIT_LABELS[unit])}
                    </MenuItem>
                  ))}
                </DropdownButton>
              </InputGroup.Button>
            </InputGroup>
          )}
        </Input>
      )}
    </Field>
  );
};

export default Time;
