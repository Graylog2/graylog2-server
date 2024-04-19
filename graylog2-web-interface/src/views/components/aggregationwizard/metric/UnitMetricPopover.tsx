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
import React, { useState } from 'react';
import { styled } from 'styled-components';
import { Field, useFormikContext, getIn } from 'formik';

import Select from 'components/common/Select';
import Popover from 'components/common/Popover';
import { IconButton } from 'components/common';
import { Input } from 'components/bootstrap';
import FieldSelect from 'views/components/aggregationwizard/FieldSelect';

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

const UnitMetricPopover = ({ index }) => {
  const [show, setShow] = useState(false);
  const toggleShow = () => setShow((cur) => !cur);

  return (
    <Popover position="right" opened={show} withArrow>
      <Popover.Target>
        <UnitButton name="line_axis"
                    onClick={toggleShow}
                    title="Unit settings" />
      </Popover.Target>
      <Popover.Dropdown title="Metrics Unit Settings">
        <Container>
          <Field name={`metrics.${index}.unit_type`}>
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <Input id="metric-unit-type-field"
                     label="Type"
                     error={error}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select id="metric-unit-type-select"
                        onChange={(fieldName) => onChange({ target: { name, value: fieldName } })}
                        name={name}
                        value={value}
                        aria-label="Select a unit type"
                        options={[]}
                        size="small" />
              </Input>
            )}
          </Field>
          <Field name={`metrics.${index}.unit`}>
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <Input id="metric-unit-field"
                     label="Unit"
                     error={error}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select id="metric-unit-select"
                        onChange={(fieldName) => onChange({ target: { name, value: fieldName } })}
                        name={name}
                        value={value}
                        aria-label="Select a unit"
                        options={[]}
                        size="small" />
              </Input>
            )}
          </Field>
        </Container>
      </Popover.Dropdown>
    </Popover>
  );
};

export default UnitMetricPopover;
