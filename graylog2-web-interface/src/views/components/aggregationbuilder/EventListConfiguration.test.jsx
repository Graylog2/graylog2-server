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
import { render, fireEvent } from 'wrappedTestingLibrary';

import EventListConfiguration from './EventListConfiguration';

describe('EventListConfiguration', () => {
  it('should render minimal', () => {
    const { container } = render(<EventListConfiguration />);

    expect(container).toMatchSnapshot();
  });

  it('should fire event onClick', () => {
    const onChange = jest.fn();
    const { getByText } = render(<EventListConfiguration onChange={onChange} />);
    const checkbox = getByText('Enable Event Annotation');

    fireEvent.click(checkbox);

    expect(onChange).toHaveBeenCalledTimes(1);
  });
});
