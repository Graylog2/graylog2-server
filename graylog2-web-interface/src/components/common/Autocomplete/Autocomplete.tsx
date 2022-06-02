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
import { Field } from 'formik';
import styled from 'styled-components';

import { Select } from 'components/common';

const StyledLabel = styled.label`
  font-weight: bold;
  margin-bottom: 5px;
  display: inline-block;
  font-size: 14px;
  background: none;
`;

const HelpText = styled.span`
  color: ${(props: any) => props.theme.colors.gray['50']};
`;

export type AutocompleteOption = {
  value: string | number,
  label: string
};

type autocompleteProps = {
  fieldName: string,
  label: string,
  clearable?: boolean,
  required?: boolean,
  helpText?: string,
  options?: AutocompleteOption[],
};

const Autocomplete = ({
  fieldName,
  label,
  clearable,
  required,
  helpText,
  options,
}: autocompleteProps) => {
  const [lOptions, setLOptions] = React.useState(options);
  React.useEffect(() => setLOptions(options), [options]);

  return (
    <Field name={fieldName} help={helpText}>
      {({ field: { name, value, onChange } }) => (
        <>
          <StyledLabel>{label}</StyledLabel>
          <Select id={fieldName}
                  name={name}
                  clearable={clearable}
                  required={required}
                  allowCreate
                  onChange={(index) => onChange({ target: { value: index, name } })}
                  options={lOptions}
                  value={value} />
          {helpText && (
            <span className="help-block">
              <HelpText>{helpText}</HelpText>
            </span>
          )}
        </>
      )}
    </Field>
  );
};

Autocomplete.defaultProps = {
  clearable: false,
  required: false,
  helpText: '',
  options: [],
};

export default Autocomplete;
