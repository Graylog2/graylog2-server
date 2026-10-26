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
import React from 'react';
import styled from 'styled-components';

import {
  ControlLabel,
  DropdownButton,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  InputWrapper,
  MenuItem,
} from 'components/bootstrap';

const OPTIONS = [
  { value: 'STRING', label: 'string' },
  { value: 'NUMBER', label: 'number' },
  { value: 'OBJECT', label: 'object' },
  { value: 'ARRAY', label: 'array' },
  { value: 'BOOLEAN', label: 'boolean' },
  { value: 'NULL', label: 'null' },
];

type JSONValueInputProps = {
  update: (...args: any[]) => void;
  onBlur?: (...args: any[]) => void;
  label?: string;
  help?: string | any;
  required?: boolean;
  validationState?: 'error' | 'success' | 'warning';
  value?: string;
  valueType?: string;
  allowedTypes?: any;
  labelClassName?: string;
  wrapperClassName?: string;
  disabled?: boolean;
};

const StyledFormGroup = styled(FormGroup)`
  padding: 0 15px;
`;

// Fully controlled: keeping a local copy of the value lets parent re-renders overwrite in-flight keystrokes.
const JSONValueInput = ({
  update,
  onBlur = undefined,
  label = '',
  help = '',
  required = false,
  validationState = undefined,
  value = '',
  valueType = 'STRING',
  allowedTypes = OPTIONS.map((option) => option.value),
  labelClassName = undefined,
  wrapperClassName = undefined,
  disabled = false,
}: JSONValueInputProps) => {
  const options = OPTIONS.filter((o) => allowedTypes.indexOf(o.value) > -1).map((o) => (
    <MenuItem key={o.value} onSelect={() => update(value, o.value)}>
      {o.label}
    </MenuItem>
  ));

  return (
    <StyledFormGroup validationState={validationState}>
      {label && <ControlLabel className={labelClassName}>{label}</ControlLabel>}
      <InputWrapper className={wrapperClassName}>
        <InputGroup>
          <FormControl
            type="text"
            onChange={(e) => update((e.target as HTMLInputElement).value, valueType)}
            onBlur={onBlur}
            value={value}
            required={required}
            disabled={disabled}
          />
          <InputGroup.Button>
            <DropdownButton
              id="input-dropdown-addon"
              bsStyle={validationState === 'error' ? 'danger' : 'default'}
              disabled={disabled}
              title={OPTIONS.filter((o) => o.value === valueType)[0].label}>
              {options}
            </DropdownButton>
          </InputGroup.Button>
        </InputGroup>
        {help && <HelpBlock>{help}</HelpBlock>}
      </InputWrapper>
    </StyledFormGroup>
  );
};

export default JSONValueInput;
