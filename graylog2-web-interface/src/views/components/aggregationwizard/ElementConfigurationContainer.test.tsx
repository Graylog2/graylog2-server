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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ElementConfigurationContainer from './ElementConfigurationContainer';

describe('ElementConfigurationContainer', () => {
  it('should render the component with children', async () => {
    render(<ElementConfigurationContainer elementTitle="element"><span>Doom</span></ElementConfigurationContainer>);

    const child = await screen.findByText('Doom');

    expect(child).toBeInTheDocument();
  });

  it('should handle onRemove button', async () => {
    const onRemove = jest.fn();
    render(<ElementConfigurationContainer onRemove={onRemove} elementTitle="element"><span>Doom</span></ElementConfigurationContainer>);

    const removeBtn = await screen.findByTitle('Remove element');
    userEvent.click(removeBtn);

    await waitFor(() => expect(onRemove).toHaveBeenCalledTimes(1));
  });
});
