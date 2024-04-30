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
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { styled } from 'styled-components';
import { Field, useFormikContext, getIn } from 'formik';

import Select from 'components/common/Select';
import Popover from 'components/common/Popover';
import { HoverForHelp, IconButton } from 'components/common';
import { Input } from 'components/bootstrap';
import type { Unit } from 'hooks/useFieldUnitTypes';
import useFieldUnitTypes from 'hooks/useFieldUnitTypes';

const UnitButton = styled(IconButton)`
  position: absolute;
  left: calc(100% + 5px);
  top: 0;
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  & .control-label {
    font-weight: normal;
  }
`;

const UnitMetricPopover = ({ index }: { index: number }) => {
  const [show, setShow] = useState(false);
  const { setFieldValue, values } = useFormikContext();
  const unitTypes = useFieldUnitTypes();
  const currentUnitType = useMemo<string>(() => values?.metrics?.[index]?.unitType, [values, index]);
  const unitTypesOptions = useMemo(() => Object.keys(unitTypes).map((key) => ({ value: key, label: key })), [unitTypes]);
  const unitOptions = useMemo(() => currentUnitType && unitTypes[currentUnitType]
    .map(({ name, abbrev }: Unit) => ({ value: name, label: abbrev })), [unitTypes, currentUnitType]);
  const toggleShow = () => setShow((cur) => !cur);
  const onUnitTypeChange = useCallback((val: string) => {
    setFieldValue(`metrics.${index}.unitType`, val);
    setFieldValue(`metrics.${index}.unit`, undefined);
  }, [index, setFieldValue]);

  useEffect(() => {
  }, []);

  return (
    <Popover position="right" opened={show} withArrow>
      <Popover.Target>
        <UnitButton name="line_axis"
                    onClick={toggleShow}
                    title="Unit settings" />
      </Popover.Target>
      <Popover.Dropdown title="Metrics Unit Settings">
        <Container>
          <Field name={`metrics.${index}.unitType`}>
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
          <Field name={`metrics.${index}.unit`}>
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <Input id="metric-unit-field"
                     label={<span>Unit <HoverForHelp displayLeftMargin>Field value unit which is used in Data Base</HoverForHelp></span>}
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
        </Container>
      </Popover.Dropdown>
    </Popover>
  );
};

export default UnitMetricPopover;
