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
import styled from 'styled-components';
import { Field } from 'formik';

import { Checkbox } from 'components/bootstrap';
import { HoverForHelp } from 'components/common';

const StyledHoverForHelp = styled(HoverForHelp)`
  margin-left: 5px;
`;

const StyledCheckbox = styled(Checkbox)`
  &.checkbox {
    padding-top: 0;
  }
`;

const Label = styled.div`
  display: flex;
  align-items: center;
`;

type Props = {
  name: string;
  label: string;
  helpTitle: string;
  children: React.ReactNode;
};

const GroupingCheckbox = ({ name, label, helpTitle, children }: Props) => (
  <Field name={name}>
    {({ field: { name: fieldName, value, onChange } }) => (
      <StyledCheckbox
        onChange={() => onChange({ target: { name: fieldName, value: !value } })}
        checked={value ?? false}>
        <Label>
          {label}
          <StyledHoverForHelp title={helpTitle}>{children}</StyledHoverForHelp>
        </Label>
      </StyledCheckbox>
    )}
  </Field>
);

export default GroupingCheckbox;
