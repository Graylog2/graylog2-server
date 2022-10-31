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
import type { Props as CreatableProps } from 'react-select/creatable';

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

type Props = CreatableProps & {
  name: string,
  values: (string | number)[],
  onChange: (newValue: React.ChangeEvent<GenericTarget<(string | number)[]>>) => void,
  size?: 'small' | 'normal',
};

const InputList = ({ name, values, onChange, size, ...rest }: Props) => {
  const { inputListTheme, styles } = useInputListStyles(size);
  const inputRef = React.useRef(null);
  const [inputValue, setInputValue] = React.useState<string>('');
  const [value, setValue] = React.useState<readonly Option[]>(values.map((val: string | number) => createOption(val)));

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
    <CreatableSelect ref={inputRef}
                     components={{ DropdownIndicator: null }}
                     inputValue={inputValue}
                     isMulti
                     menuIsOpen={false}
                     onChange={handleOnChange}
                     onInputChange={(newValue: string) => setInputValue(newValue)}
                     onKeyDown={handleKeyDown}
                     value={value}
                     styles={styles}
                     theme={inputListTheme}
                     {...rest} />
  );
};

InputList.defaultProps = {
  size: 'normal',
};

export default InputList;
