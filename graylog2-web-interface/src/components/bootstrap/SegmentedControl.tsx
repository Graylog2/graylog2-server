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
import type { MantineRadius, SegmentedControlItem } from '@mantine/core';

type Props<OptionValue> = {
  data: Array<SegmentedControlItem & { value: OptionValue }>,
  defaultValue?: string,
  disabled?: boolean,
  onChange?: (value: OptionValue) => void,
  value?: OptionValue,
  className?: string,
  radius?: MantineRadius,
}

const SegmentedControl = <OptionValue extends string>({ className, data, defaultValue, disabled = false, onChange, value, radius = 'xs' }: Props<OptionValue>) => {
  const theme = useTheme();

  return (
    <MantineSegmentedControl color={theme.colors.variant.info}
                             className={className}
                             data={data}
                             defaultValue={defaultValue}
                             radius={radius}
                             disabled={disabled}
                             value={value}
                             onChange={onChange}
                             styles={{ label: { marginBottom: 0 } }} />
  );
};

export default SegmentedControl;
