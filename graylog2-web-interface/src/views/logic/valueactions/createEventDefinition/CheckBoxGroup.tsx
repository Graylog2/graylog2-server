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
import React, { useCallback, useMemo } from 'react';
import styled from 'styled-components';
import mapValues from 'lodash/mapValues';

import { Checkbox } from 'components/bootstrap';
import type { Checked } from 'views/logic/valueactions/createEventDefinition/types';

const CHECKBOX_STATES = {
  Checked: 'Checked',
  Empty: 'Empty',
  Indeterminate: 'Indeterminate',
};

type CheckboxProps = {
  label: string,
  value: string,
  onChange: () => void,
}

const NestedWrapper = styled.div`
  margin-left: 15px;
`;

const CheckboxWithIndeterminate = ({ label, children, value, onChange }: React.PropsWithChildren<CheckboxProps>) => {
  const checkboxRef = React.useRef(null);

  React.useEffect(() => {
    if (value === CHECKBOX_STATES.Checked) {
      checkboxRef.current.checked = true;
      checkboxRef.current.indeterminate = false;
    } else if (value === CHECKBOX_STATES.Empty) {
      checkboxRef.current.checked = false;
      checkboxRef.current.indeterminate = false;
    } else if (value === CHECKBOX_STATES.Indeterminate) {
      checkboxRef.current.checked = false;
      checkboxRef.current.indeterminate = true;
    }
  }, [value]);

  return (
    <Checkbox label={label} checked={value === CHECKBOX_STATES.Checked} inputRef={(ref) => { checkboxRef.current = ref; }} onChange={onChange}>{children}</Checkbox>
  );
};

type Props = {
  groupLabel: string,
  checked: Checked,
  labels: { [name: string]: JSX.Element }
  onChange: (Checked) => void
}

const CheckBoxGroup = ({ groupLabel, checked, onChange, labels }: Props) => {
  const value = useMemo(() => {
    if (Object.values(checked).every((val) => val)) {
      return CHECKBOX_STATES.Checked;
    }

    if (Object.values(checked).some((val) => val)) {
      return CHECKBOX_STATES.Indeterminate;
    }

    return CHECKBOX_STATES.Empty;
  }, [checked]);

  const groupOnChange = useCallback(() => {
    switch (value) {
      case CHECKBOX_STATES.Checked:
        return onChange(mapValues(checked, () => false));
      case CHECKBOX_STATES.Empty:
        return onChange(mapValues(checked, () => true));
      case CHECKBOX_STATES.Indeterminate:
        return onChange(mapValues(checked, () => true));
      default:
        return onChange(mapValues(checked, () => false));
    }
  }, [checked, onChange, value]);

  const itemOnChange = useCallback((key: string | number, _?: React.SyntheticEvent) => {
    onChange({
      ...checked,
      [key]: !checked[key],
    });
  }, [checked, onChange]);

  return (
    <div>
      <CheckboxWithIndeterminate label={groupLabel} value={value} onChange={groupOnChange}><b>{groupLabel}</b></CheckboxWithIndeterminate>
      <div>
        {
          Object.entries(checked).map(([key, val]) => (
            <NestedWrapper key={key}>
              <Checkbox checked={val} onChange={(e) => itemOnChange(key, e)}>
                {labels[key]}
              </Checkbox>
            </NestedWrapper>
          ))
        }
      </div>
    </div>
  );
};

export default CheckBoxGroup;
