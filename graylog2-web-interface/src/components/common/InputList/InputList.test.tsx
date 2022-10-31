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
import { components } from 'react-select';
import type { InputProps, MultiValueRemoveProps, ClearIndicatorProps } from 'react-select';
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import InputList from './InputList';

const renderComponent = (onChange: (e: React.BaseSyntheticEvent) => void, values: string[] = []) => {
  const Input = ({ ...props }: InputProps) => <components.Input title="testListInput" {...props} />;
  const MultiValueRemove = ({ ...props }: MultiValueRemoveProps) => (
    <components.MultiValueRemove {...props}>value remover</components.MultiValueRemove>
  );

  const CustomClearText: React.FC = () => <>clear all</>;

  const ClearIndicator = (props: ClearIndicatorProps) => {
    const {
      children = <CustomClearText />,
      innerProps: { ref, ...restInnerProps },
    } = props;

    return (
      <div {...restInnerProps} ref={ref}><div>{children}</div></div>
    );
  };

  return render(
    <InputList id="testList"
               name="testList"
               values={values}
               isClearable
               onChange={onChange}
               components={{ Input, MultiValueRemove, ClearIndicator }} />,
  );
};

describe('InputList Component', () => {
  it('should list the provided values', () => {
    renderComponent(() => {}, ['dir1/', 'dir2/']);

    expect(screen.getByText(/dir1/i)).toBeVisible();
    expect(screen.getByText(/dir2/i)).toBeVisible();
  });

  it('should add a value when input has value and [TAB] or [Enter] is pressed', () => {
    renderComponent(() => {});
    const rawInput = screen.getByTitle(/testListInput/i);

    fireEvent.change(rawInput, { target: { value: 'dir3' } });
    fireEvent.keyDown(rawInput, { key: 'Tab' });

    fireEvent.change(rawInput, { target: { value: 'dir4' } });
    fireEvent.keyDown(rawInput, { key: 'Enter' });

    expect(screen.getByText(/dir3/i)).toBeVisible();
    expect(screen.getByText(/dir4/i)).toBeVisible();
  });

  it('should remove the last value added when [Backspace] is pressed', () => {
    renderComponent(() => {}, ['dir3', 'dir4']);
    const rawInput = screen.getByTitle(/testListInput/i);

    expect(screen.getByText(/dir3/i)).toBeVisible();
    expect(screen.getByText(/dir4/i)).toBeVisible();

    fireEvent.keyDown(rawInput, { key: 'Backspace' });

    expect(screen.getByText(/dir3/i)).toBeVisible();
    expect(screen.queryByText(/dir4/i)).not.toBeInTheDocument();
  });

  it('should remove a value when clicking the "X" on the value bubble', () => {
    renderComponent(() => {}, ['dir3', 'dir4']);
    let removers = screen.getAllByText(/value\s*remover/i);

    expect(screen.getByText(/dir3/i)).toBeVisible();
    expect(screen.getByText(/dir4/i)).toBeVisible();

    fireEvent.click(removers[0]);

    removers = screen.getAllByText(/value\s*remover/i);

    expect(removers.length).toEqual(1);
    expect(screen.getByText(/dir4/i)).toBeVisible();
  });

  it('should clear all values when clicking the clear trigger "X"', () => {
    renderComponent((e: React.BaseSyntheticEvent) => { console.log(e.target.value); }, ['dir5', 'dir6', 'dir7']);
    const clearAll = screen.getByText(/clear\s*all/i);

    expect(screen.getByText(/dir5/i)).toBeVisible();
    expect(screen.getByText(/dir6/i)).toBeVisible();
    expect(screen.getByText(/dir7/i)).toBeVisible();

    fireEvent.click(clearAll);
    clearAll.click();

    screen.debug();

    expect(screen.getByText(/dir5/i)).toBeVisible();
  });

  it.skip('should return an event with a target an a list of values of the requested type', () => {
    expect(true).toBeTruthy();
  });
});
