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
import { useLayoutEffect, useRef, useState } from 'react';
import CreatableSelect from 'react-select/creatable';
import type { CreatableProps } from 'react-select/creatable';

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

type Props = CreatableProps<any, boolean, any> & {
  name: string;
  values: (string | number)[];
  onChange: (newValue: React.ChangeEvent<GenericTarget<(string | number)[]>>) => void;
  label?: React.ReactNode;
  size?: 'small' | 'normal';
  bsStyle?: 'success' | 'warning' | 'error' | null;
  error?: React.ReactNode;
  help?: React.ReactNode;
  // When `suggestions` is provided, the dropdown menu opens on input. When omitted,
  // existing consumers are unaffected: no menu, no dropdown indicator.
  suggestions?: ReadonlyArray<string | number>;
  onSuggestionsInputChange?: (input: string) => void;
  isLoadingSuggestions?: boolean;
  // Chips whose value is in this set render with the variant.danger border/text color
  // so the user can see which specific entries failed validation.
  invalidValues?: ReadonlySet<string | number>;
};

const InputList = ({
  name,
  values,
  onChange,
  label = null,
  size = 'normal',
  bsStyle = null,
  error = null,
  help = null,
  suggestions = undefined,
  onSuggestionsInputChange = undefined,
  isLoadingSuggestions = undefined,
  invalidValues = undefined,
  ...rest
}: Props) => {
  const { inputListTheme, styles } = useInputListStyles(size);
  const inputRef = useRef(null);
  const [inputValue, setInputValue] = useState<string>('');
  const [value, setValue] = useState<readonly Option[]>(values.map((val: string | number) => createOption(val)));

  useLayoutEffect(() => setValue(values.map((val: string | number) => createOption(val))), [values]);

  const suggestionsEnabled = suggestions !== undefined;
  const options = suggestionsEnabled ? suggestions.map(createOption) : undefined;

  const dispatchOnChange = (newValue: Option[]) => {
    const newList = newValue.map((item: Option) => item.value);
    const event = new GenericChangeEvent<(string | number)[]>('change');

    inputRef.current.value = newList;
    inputRef.current.name = name;
    event.target = inputRef.current;

    onChange(event);
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    // When suggestions are enabled, defer to react-select's native Enter/Tab handling so the
    // menu stays open after committing a value (isMulti behavior).
    if (suggestionsEnabled) return;
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

  const handleInputChange = (newValue: string) => {
    setInputValue(newValue);
    onSuggestionsInputChange?.(newValue);
  };

  return (
    <FormGroup controlId={rest.id ? rest.id : name} validationState={error ? 'error' : bsStyle}>
      {label && <ControlLabel>{label}</ControlLabel>}
      <CreatableSelect
        ref={inputRef}
        components={suggestionsEnabled ? undefined : { DropdownIndicator: null }}
        inputValue={inputValue}
        isMulti
        menuIsOpen={suggestionsEnabled ? undefined : false}
        options={options}
        isLoading={isLoadingSuggestions}
        formatCreateLabel={suggestionsEnabled ? (input) => `Add "${input}"` : undefined}
        closeMenuOnSelect={suggestionsEnabled ? false : undefined}
        onChange={handleOnChange}
        onInputChange={handleInputChange}
        onKeyDown={handleKeyDown}
        value={value}
        styles={styles(!error, invalidValues)}
        theme={(theme) => ({ ...theme, ...inputListTheme })}
        {...rest}
      />
      <InputDescription error={error} help={help} />
    </FormGroup>
  );
};

export default InputList;
