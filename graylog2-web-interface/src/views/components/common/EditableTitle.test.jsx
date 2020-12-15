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
import { render, screen } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/dom';

import EditableTitle from './EditableTitle';

describe('EditableTitle', () => {
  it('stops submit event propagation', () => {
    const onSubmit = jest.fn((e) => e.persist());

    render((
      <div onSubmit={onSubmit}>
        <EditableTitle value="Current title" onChange={jest.fn()} />
      </div>
    ));

    const currentTitle = screen.getByText('Current title');
    fireEvent.dblClick(currentTitle);

    const titleInput = screen.getByRole('textbox');
    fireEvent.change(titleInput, { target: { value: 'New title' } });
    fireEvent.submit(titleInput);

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
