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

import ElementConfigurationContainer from './ElementConfigurationContainer';

describe('ElementConfigurationContainer', () => {
  it('should render elements passed as children', () => {
    render(
      <ElementConfigurationContainer allowAddEmptyElement
                                     onAddEmptyElement={() => {}}
                                     elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    expect(screen.getByText('Children of Dune')).toBeInTheDocument();
  });

  it('should render title', () => {
    render(
      <ElementConfigurationContainer allowAddEmptyElement
                                     onAddEmptyElement={() => {}}
                                     elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    expect(screen.getByText('Aggregation Element Title')).toBeInTheDocument();
  });

  it('should call on onAddEmptyElement when adding a section', async () => {
    const onAddEmptyElementMock = jest.fn();

    render(
      <ElementConfigurationContainer allowAddEmptyElement
                                     onAddEmptyElement={onAddEmptyElementMock}
                                     elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    const addButton = screen.getByTitle('Add a Aggregation Element Title');

    fireEvent.click(addButton);

    expect(onAddEmptyElementMock).toHaveBeenCalledTimes(1);
  });

  it('should not display add section icon if adding element section is not allowed', async () => {
    render(
      <ElementConfigurationContainer allowAddEmptyElement={false}
                                     onAddEmptyElement={() => {}}
                                     elementTitle="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    expect(screen.queryByTitle('Add a Aggregation Element Title')).not.toBeInTheDocument();
  });
});
