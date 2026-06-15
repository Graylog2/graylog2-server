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

type Props = {
  checked?: boolean;
  children?: React.ReactNode;
  className?: string;
  disabled?: boolean;
  id?: string;
  indeterminate?: boolean;
  inline?: boolean;
  name?: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onClick?: React.MouseEventHandler<HTMLInputElement>;
  readOnly?: boolean;
  title?: string;
};

const Checkbox = (
  {
    checked = undefined,
    children = undefined,
    className = undefined,
    disabled = undefined,
    id = undefined,
    indeterminate = false,
    inline = false,
    name = undefined,
    onChange,
    onClick = undefined,
    readOnly = undefined,
    title = undefined,
  }: Props,
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
    <label className={inline ? `checkbox-inline ${className ?? ''}` : undefined} title={title} htmlFor={id}>
      {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
      <input
        type="checkbox"
        ref={checkboxRef}
        id={id}
        name={name}
        checked={checked}
        disabled={disabled}
        readOnly={readOnly}
        onClick={onClick}
        onChange={onChange}
      />
      {children}
    </label>
  );

  return inline ? label : <div className={`checkbox ${className ?? ''}`}>{label}</div>;
};

export default React.forwardRef(Checkbox);
