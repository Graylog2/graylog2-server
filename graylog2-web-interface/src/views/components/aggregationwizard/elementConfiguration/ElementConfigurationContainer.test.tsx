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
      <ElementConfigurationContainer isPermanentElement={false}
                                     onDeleteAll={() => {}}
                                     title="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    expect(screen.getByText('Children of Dune')).toBeInTheDocument();
  });

  it('should render title', () => {
    render(
      <ElementConfigurationContainer isPermanentElement={false}
                                     onDeleteAll={() => {}}
                                     title="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    expect(screen.getByText('Aggregation Element Title')).toBeInTheDocument();
  });

  it('should call on delete when clicking delete icon', async () => {
    const onDeleteAllMock = jest.fn();

    render(
      <ElementConfigurationContainer isPermanentElement={false}
                                     onDeleteAll={onDeleteAllMock}
                                     title="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    const deleteButton = screen.getByTitle('Remove Aggregation Element Title');

    fireEvent.click(deleteButton);

    expect(onDeleteAllMock).toHaveBeenCalledTimes(1);
  });

  it('should not display delete icon if element is permanent', async () => {
    render(
      <ElementConfigurationContainer isPermanentElement
                                     onDeleteAll={() => {}}
                                     title="Aggregation Element Title">
        Children of Dune
      </ElementConfigurationContainer>,
    );

    expect(screen.queryByTitle('Remove Aggregation Element Title')).not.toBeInTheDocument();
  });
});
