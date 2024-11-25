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
import React, { useCallback, useMemo, useState } from 'react';
import { styled, css } from 'styled-components';
import { Field, useFormikContext } from 'formik';
import capitalize from 'lodash/capitalize';

import Select from 'components/common/Select';
import Popover from 'components/common/Popover';
import { HoverForHelp, ModalButtonToolbar } from 'components/common';
import { Alert, Button, Input } from 'components/bootstrap';
import type { Unit } from 'views/components/visualizations/utils/unitConverters';
import { mappedUnitsFromJSON as units } from 'views/components/visualizations/utils/unitConverters';
import type { FieldUnitsFormValues } from 'views/types';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import getUnitTextLabel from 'views/components/visualizations/utils/getUnitTextLabel';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .control-label {
    font-weight: normal;
  }

  width: 300px;
`;

const ButtonContainer = styled.div`
  display: flex;
  width: 100%;
  justify-content: center;
  align-items: center;
  height: 25px;
`;

export const StyledButton = styled(Button)(({ theme }) => css`
  background-color: ${theme.colors.gray[60]};
  padding: 1px 2px;
  min-width: 20px;
  border-radius: 3px;
  color: ${theme.colors.variant.lightest.default};

  &:hover {
    background-color: ${theme.colors.gray[80]};
    color: ${theme.colors.variant.lightest.default};
  }
`);

const FieldUnitPopover = ({ field, predefinedUnit }: { field: string, predefinedUnit: FieldUnit }) => {
  const [show, setShow] = useState(false);
  const { setFieldValue, values } = useFormikContext<{units: FieldUnitsFormValues }>();
  const currentUnitType = useMemo<string>(() => values?.units?.[field]?.unitType, [values, field]);
  const unitTypesOptions = useMemo(() => Object.keys(units).map((key) => ({ value: key, label: capitalize(key) })), []);
  const unitOptions = useMemo(() => currentUnitType && units[currentUnitType]
    .map(({ abbrev, name }: Unit) => ({ value: abbrev, label: capitalize(name) })), [currentUnitType]);
  const toggleShow = () => setShow((cur) => !cur);
  const onUnitTypeChange = useCallback((val: string) => {
    setFieldValue(`units.${field}`, { unitType: val || undefined, abbrev: undefined });
  }, [field, setFieldValue]);

  const badgeLabel = useMemo(() => {
    const curUnit = values?.units?.[field]?.abbrev;

    return getUnitTextLabel(curUnit) || '...';
  }, [field, values?.units]);

  const predefinedInfo = useMemo(() => {
    if (!predefinedUnit?.isDefined) return null;

    const unitName = units[predefinedUnit.unitType].find(({ abbrev }) => abbrev === predefinedUnit?.abbrev).name;

    return <>Unit <b>{unitName}</b> was defined for field <b>{field}</b> by Graylog. Changing this unit might represent data incorrectly on the charts</>;
  }, [field, predefinedUnit?.abbrev, predefinedUnit?.isDefined, predefinedUnit?.unitType]);

  const onClear = useCallback(() => {
    setFieldValue(`units.${field}`, undefined);
    toggleShow();
  }, [field, setFieldValue]);

  return (
    <Popover position="right" opened={show} withArrow>
      <Popover.Target>
        <ButtonContainer>
          <StyledButton bsSize="xs" onClick={toggleShow} title={`${field} unit settings`}>{badgeLabel}</StyledButton>
        </ButtonContainer>
      </Popover.Target>
      <Popover.Dropdown title={`${field} unit settings`}>
        <Container>
          <Field name={`units.${field}.unitType`}>
            {({ field: { name, value }, meta: { error } }) => (
              <Input id="metric-unit-type-field"
                     label="Type"
                     error={error}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select id="metric-unit-type-select"
                        onChange={onUnitTypeChange}
                        name={name}
                        value={value}
                        aria-label="Select a unit type"
                        options={unitTypesOptions}
                        size="small" />
              </Input>
            )}
          </Field>
          {currentUnitType && (
          <Field name={`units.${field}.abbrev`}>
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <Input id="metric-unit-field"
                     label={<span>Unit <HoverForHelp displayLeftMargin>Unit which is used to format values of metric in charts</HoverForHelp></span>}
                     error={error}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select id="metric-unit-select"
                        onChange={(fieldName) => onChange({ target: { name, value: fieldName } })}
                        name={name}
                        value={value}
                        aria-label="Select a unit"
                        options={unitOptions}
                        size="small" />
              </Input>
            )}
          </Field>
          )}
          {predefinedInfo && <Alert bsStyle="info">{predefinedInfo}</Alert>}
          <ModalButtonToolbar>
            <Button bsSize="xs" onClick={onClear}>Clear</Button>
            <Button bsSize="xs" bsStyle="success" onClick={toggleShow}>OK</Button>
          </ModalButtonToolbar>
        </Container>
      </Popover.Dropdown>
    </Popover>
  );
};

export default FieldUnitPopover;
