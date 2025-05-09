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
import { useImperativeHandle, useState } from 'react';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

export const Container = styled.div`
  width: 100%;
`;

export type TypeAheadInputRef = {
  getValue: () => string;
  clear: () => void;
};
type TypeAheadInputProps = {
  /** ID to use in the input field. */
  id: string;
  /** Label to use for the input field. */
  label: string;
  /** String that allows overriding the input form group */
  formGroupClassName?: string;
};

/**
 * Component that renders a field input with auto-completion capabilities.
 *
 * **Note** There are a few quirks around this component and it will be
 * refactored soon.
 */

const TypeAheadInput = (
  { id, label, formGroupClassName = undefined }: TypeAheadInputProps,
  ref: React.Ref<TypeAheadInputRef>,
) => {
  const [value, setValue] = useState('');
  useImperativeHandle(
    ref,
    () => ({
      getValue: () => value,
      clear: () => setValue(''),
    }),
    [value, setValue],
  );

  return (
    <Container>
      <Input
        id={id}
        type="text"
        wrapperClassName="typeahead-wrapper"
        formGroupClassName={formGroupClassName}
        label={label}
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
    </Container>
  );
};

export default React.forwardRef(TypeAheadInput);
