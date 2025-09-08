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
import mapValues from 'lodash/mapValues';

import type { Checked } from 'views/logic/valueactions/createEventDefinition/types';
import { ExpandableCheckboxListItem } from 'components/common';
import { Input } from 'components/bootstrap';

const CHECKBOX_STATES = {
  Checked: 'Checked',
  Empty: 'Empty',
  Indeterminate: 'Indeterminate',
};

type Props = {
  groupLabel: string;
  checked: Checked;
  labels: { [name: string]: JSX.Element };
  onChange: (newChecked: Checked) => void;
};

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

  const itemOnChange = useCallback(
    (key: string | number, _?: React.SyntheticEvent) => {
      onChange({
        ...checked,
        [key]: !checked[key],
      });
    },
    [checked, onChange],
  );

  return (
    <ExpandableCheckboxListItem
      header={groupLabel}
      value={groupLabel}
      checked={value === CHECKBOX_STATES.Checked}
      indeterminate={value === CHECKBOX_STATES.Indeterminate}
      onChange={groupOnChange}>
      {Object.entries(checked).map(([key, isChecked]) => (
        <Input
          type="checkbox"
          label={labels[key]}
          formGroupClassName="no-bm"
          key={key}
          checked={isChecked}
          onChange={(e) => itemOnChange(key, e)}
        />
      ))}
    </ExpandableCheckboxListItem>
  );
};

export default CheckBoxGroup;
