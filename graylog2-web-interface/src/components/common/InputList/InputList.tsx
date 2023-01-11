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
import CreatableSelect from 'react-select/creatable';

import { InputDescription } from 'components/common';
import { FormGroup, ControlLabel } from 'components/bootstrap';

import useInputListStyles from './useInputListStyles';
import { GenericChangeEvent } from './InputList.types';
import type { GenericTarget } from './InputList.types';

interface Option {
  readonly label: string | number;
  readonly value: string | number;
}

const createOption = (value: string | number) => ({
  label: value,
  value,
});

type Props = {
  bsStyle?: 'success' | 'warning' | 'error' | null,
  components?: {
    Input?: React.ComponentType<any>,
    MultiValueRemove?: React.ComponentType<any>,
    ClearIndicator?: React.ComponentType<any>,
  },
  error?: string | React.ReactHTMLElement<HTMLElement> | null,
  help?: string | React.ReactHTMLElement<HTMLElement> | null,
  id: string,
  isClearable?: boolean,
  label?: React.ReactNode,
  name: string,
  onChange: (newValue: React.ChangeEvent<GenericTarget<(string | number)[]>>) => void,
  placeholder?: string,
  size?: 'small' | 'normal',
  values: (string | number)[],
};

const InputList = ({
  name,
  values,
  onChange,
  label,
  size,
  bsStyle,
  error,
  help,
  id,
  isClearable,
  components,
  placeholder,
}: Props) => {
  const { inputListTheme, styles } = useInputListStyles(size);
  const inputRef = React.useRef(null);
  const [inputValue, setInputValue] = React.useState<string>('');
  const [value, setValue] = React.useState<readonly Option[]>(values.map((val: string | number) => createOption(val)));

  React.useLayoutEffect(() => setValue(values.map((val: string | number) => createOption(val))), [values]);

  const dispatchOnChange = (newValue: Option[]) => {
    const newList = newValue.map((item: Option) => item.value);
    const event = new GenericChangeEvent<(string | number)[]>('change');

    inputRef.current.value = newList;
    inputRef.current.name = name;
    event.target = inputRef.current;

    onChange(event);
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (!inputValue) return;

    if (event.key === 'Enter' || event.key === 'Tab') {
      const newValue = [...value, createOption(inputValue)];
      setValue(newValue);
      dispatchOnChange(newValue);
      setInputValue('');
      event.preventDefault();
    }
  };

  const handleOnChange = (newValue: Option[]) => {
    setValue(newValue);
    dispatchOnChange(newValue);
  };

  return (
    <FormGroup controlId={id ?? name} validationState={error ? 'error' : bsStyle}>
      {label && <ControlLabel>{label}</ControlLabel>}
      <CreatableSelect ref={inputRef}
                       id={id}
                       components={{ DropdownIndicator: null, ...(components ?? {}) }}
                       inputValue={inputValue}
                       isMulti
                       isClearable={isClearable}
                       placeholder={placeholder}
                       menuIsOpen={false}
                       onChange={handleOnChange}
                       onInputChange={(newValue: string) => setInputValue(newValue)}
                       onKeyDown={handleKeyDown}
                       value={value}
                       styles={styles(!error)}
                       theme={(theme) => ({ ...theme, ...inputListTheme })} />
      <InputDescription error={error} help={help} />
    </FormGroup>
  );
};

InputList.defaultProps = {
  label: null,
  size: 'normal',
  bsStyle: null,
  error: null,
  help: null,
  placeholder: undefined,
  isClearable: false,
  components: undefined,
};

export default InputList;
