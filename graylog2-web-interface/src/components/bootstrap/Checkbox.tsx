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
// eslint-disable-next-line no-restricted-imports
import { Checkbox as BootstrapCheckbox } from 'react-bootstrap';
import { useEffect, useRef } from 'react';

type BootstrapCheckboxProps = React.ComponentProps<typeof BootstrapCheckbox>;

type Props = Omit<BootstrapCheckboxProps, 'onChange' | 'inputRef'> & {
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  indeterminate?: boolean;
};
const Checkbox = (
  { onChange, indeterminate = false, ...props }: Props,
  forwardedRef: React.MutableRefObject<HTMLInputElement>,
) => {
  const internalRef = useRef<HTMLInputElement>(null);
  const checkboxRef = forwardedRef || internalRef;

  useEffect(() => {
    if (checkboxRef.current && checkboxRef.current.indeterminate !== indeterminate) {
      checkboxRef.current.indeterminate = indeterminate;
    }
  }, [checkboxRef, indeterminate]);

  return (
    <BootstrapCheckbox
      onChange={onChange as unknown as BootstrapCheckboxProps['onChange']}
      inputRef={(ref) => {
        checkboxRef.current = ref;
      }}
      {...props}
    />
  );
};

export default React.forwardRef(Checkbox);
