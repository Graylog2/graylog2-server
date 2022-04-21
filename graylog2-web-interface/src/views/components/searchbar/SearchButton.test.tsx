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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import SearchButton from 'views/components/searchbar/SearchButton';

describe('SearchButton', () => {
  const onFormSubmit = jest.fn().mockImplementation((e) => e.preventDefault());

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const SUT = ({ disabled, dirty }: { dirty?: boolean, disabled?: boolean}) => (
    <form onSubmit={onFormSubmit}>
      <SearchButton disabled={disabled} dirty={dirty} />
    </form>
  );

  SUT.defaultProps = {
    dirty: false,
    disabled: false,
  };

  it('should trigger form submit refresh when dirty', () => {
    render(<SUT dirty />);

    const button = screen.getByTitle('Perform search (changes were made after last search execution)');

    fireEvent.click(button);

    expect(onFormSubmit).toHaveBeenCalledTimes(1);
  });

  it('should trigger form submit refresh when not dirty', () => {
    render(<SUT />);

    const button = screen.getByTitle('Perform search');

    fireEvent.click(button);

    expect(onFormSubmit).toHaveBeenCalledTimes(1);
  });
});
