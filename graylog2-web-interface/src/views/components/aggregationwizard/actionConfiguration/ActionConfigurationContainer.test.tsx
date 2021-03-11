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

import ActionConfigurationContainer from './ActionConfigurationContainer';

describe('ActionConfigurationContainer', () => {
  it('should render actions passed as children', () => {
    render(
      <ActionConfigurationContainer isPermanentAction={false}
                                    onDeleteAll={() => {}}
                                    title="Aggregation Action Title">
        Children of Dune
      </ActionConfigurationContainer>,
    );

    expect(screen.getByText('Children of Dune')).toBeInTheDocument();
  });

  it('should render title', () => {
    render(
      <ActionConfigurationContainer isPermanentAction={false}
                                    onDeleteAll={() => {}}
                                    title="Aggregation Action Title">
        Children of Dune
      </ActionConfigurationContainer>,
    );

    expect(screen.getByText('Aggregation Action Title')).toBeInTheDocument();
  });

  it('should call on delete when clicking delete icon', async () => {
    const onDeleteAllMock = jest.fn();

    render(
      <ActionConfigurationContainer isPermanentAction={false}
                                    onDeleteAll={onDeleteAllMock}
                                    title="Aggregation Action Title">
        Children of Dune
      </ActionConfigurationContainer>,
    );

    const deleteButton = screen.getByTitle('Remove Aggregation Action Title');

    fireEvent.click(deleteButton);

    expect(onDeleteAllMock).toHaveBeenCalledTimes(1);
  });

  it('should not display delete icon if action is permanent', async () => {
    render(
      <ActionConfigurationContainer isPermanentAction
                                    onDeleteAll={() => {}}
                                    title="Aggregation Action Title">
        Children of Dune
      </ActionConfigurationContainer>,
    );

    expect(screen.queryByTitle('Remove Aggregation Action Title')).not.toBeInTheDocument();
  });
});
