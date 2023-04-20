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
import { ExpandableList, ExpandableListItem } from 'components/common';

const CHECKBOX_STATES = {
  Checked: 'Checked',
  Empty: 'Empty',
  Indeterminate: 'Indeterminate',
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
    <ExpandableListItem header={groupLabel}
                        expanded
                        padded={false}
                        checked={value === CHECKBOX_STATES.Checked}
                        indetermined={value === CHECKBOX_STATES.Indeterminate}
                        onChange={groupOnChange}>
      <ExpandableList>
        {
            Object.entries(checked).map(([key, isChecked]) => (
              <ExpandableListItem expandable={false}
                                  header={labels[key]}
                                  padded={false}
                                  key={key}
                                  checked={isChecked}
                                  onChange={(e) => itemOnChange(key, e)} />
            ))
          }
      </ExpandableList>
    </ExpandableListItem>
  );
};

export default CheckBoxGroup;
