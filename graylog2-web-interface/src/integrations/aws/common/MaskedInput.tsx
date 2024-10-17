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
import React, { useState } from 'react';
import styled from 'styled-components';

import { Icon } from 'components/common';

import ValidatedInput from './ValidatedInput';

type MaskedInputProps = React.ComponentProps<typeof ValidatedInput> & {
  label: string | React.ReactNode;
  className?: string;
};

const MaskedInput = ({
  className,
  label,
  ...props
}: MaskedInputProps) => {
  const [masked, setMasked] = useState(true);
  const toggleLabel = (
    <LabelWrapper>
      {label}
      <ToggleMask onClick={() => setMasked(!masked)} aria-description={`Toggle ${label} field input`}>
        <Icon name={masked ? 'visibility_off' : 'visibility'} />
      </ToggleMask>
    </LabelWrapper>
  );

  return (
    <ValidatedInput {...props} type={masked ? 'password' : 'text'} label={toggleLabel} formGroupClassName={className} />
  );
};

const LabelWrapper = styled.span`
  display: flex;
  align-items: center;
`;

const ToggleMask = styled.button`
  border: 0;
  background: none;
  padding: 0;
  margin: 0 0 0 12px;
`;

export default MaskedInput;
