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
import { useEffect, useRef } from 'react';

type Props = Omit<React.InputHTMLAttributes<HTMLInputElement>, 'onChange' | 'type'> & {
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  indeterminate?: boolean;
  inline?: boolean;
  children?: React.ReactNode;
  className?: string;
  title?: string;
};

const Checkbox = (
  { onChange, indeterminate = false, inline = false, children = undefined, className = undefined, title = undefined, ...props }: Props,
  forwardedRef: React.MutableRefObject<HTMLInputElement>,
) => {
  const internalRef = useRef<HTMLInputElement>(null);
  const checkboxRef = forwardedRef || internalRef;

  useEffect(() => {
    if (checkboxRef.current && checkboxRef.current.indeterminate !== indeterminate) {
      checkboxRef.current.indeterminate = indeterminate;
    }
  }, [checkboxRef, indeterminate]);

  const label = (
    <label className={inline ? `checkbox-inline ${className ?? ''}` : undefined} title={title}>
      <input
        type="checkbox"
        ref={checkboxRef}
        onChange={onChange}
        {...props}
      />
      {children}
    </label>
  );

  return inline ? label : <div className={`checkbox ${className ?? ''}`}>{label}</div>;
};

export default React.forwardRef(Checkbox);
