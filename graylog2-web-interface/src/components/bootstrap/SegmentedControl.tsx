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
import { SegmentedControl as MantineSegmentedControl } from '@mantine/core';
import { useTheme } from 'styled-components';
import type { SegmentedControlItem } from '@mantine/core';

type Props<OptionValue> = {
  data: Array<SegmentedControlItem & { value: OptionValue }>,
  defaultValue?: string,
  disabled?: boolean,
  onChange?: (value: OptionValue) => void,
  value?: OptionValue,
}

const SegmentedControl = <OptionValue extends string>({ data, defaultValue, disabled, onChange, value }: Props<OptionValue>) => {
  const theme = useTheme();

  return (
    <MantineSegmentedControl color={theme.colors.variant.info}
                             data={data}
                             defaultValue={defaultValue}
                             disabled={disabled}
                             value={value}
                             onChange={onChange}
                             styles={{ label: { marginBottom: 0 } }} />
  );
};

SegmentedControl.defaultProps = {
  defaultValue: undefined,
  disabled: false,
  onChange: undefined,
  value: undefined,
};

export default SegmentedControl;
