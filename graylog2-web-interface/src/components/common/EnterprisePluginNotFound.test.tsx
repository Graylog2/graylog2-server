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

import EnterprisePluginNotFound from './EnterprisePluginNotFound';

describe('EnterprisePluginNotFound', () => {
  it('explains what is required to use the feature', async () => {
    render(<EnterprisePluginNotFound featureName="Archiving" />);

    await screen.findByText('Enterprise Feature');

    expect(screen.getByText('Archiving')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Enterprise license/ })).toHaveAttribute(
      'href',
      'https://www.graylog.org/products/enterprise',
    );
    expect(screen.getByRole('link', { name: /Enterprise plugin/ })).toBeInTheDocument();
  });
});
