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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';

import ElementConfigurationSection from './ElementConfigurationSection';

describe('ElementConfigurationSection', () => {
  it('should render elements passed as children', () => {
    render(
      <ElementConfigurationSection allowCreate
                                   onCreate={() => {}}
                                   elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationSection>,
    );

    expect(screen.getByText('Children of Dune')).toBeInTheDocument();
  });

  it('should render title', () => {
    render(
      <ElementConfigurationSection allowCreate
                                   onCreate={() => {}}
                                   elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationSection>,
    );

    expect(screen.getByText('Aggregation Element Title')).toBeInTheDocument();
  });

  it('should call on onCreate when adding a section', async () => {
    const onCreateMock = jest.fn();

    render(
      <ElementConfigurationSection allowCreate
                                   onCreate={onCreateMock}
                                   elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationSection>,
    );

    const addButton = screen.getByTitle('Add a Aggregation Element Title');

    fireEvent.click(addButton);

    expect(onCreateMock).toHaveBeenCalledTimes(1);
  });

  it('should not display add section icon if adding element section is not allowed', async () => {
    render(
      <ElementConfigurationSection allowCreate={false}
                                   onCreate={() => {}}
                                   elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationSection>,
    );

    expect(screen.queryByTitle('Add a Aggregation Element Title')).not.toBeInTheDocument();
  });
});
