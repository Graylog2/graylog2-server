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
import { renderWithDataRouter } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import RulesPage from './RulesPage';

describe('RulesPage', () => {
  it('should show 2 options on create rule: Rule Bulder and Source Code', () => {
    const { getByRole, getByText } = renderWithDataRouter(<RulesPage />);

    const createRuleButton = getByRole('button', { name: 'Create Rule' });
    userEvent.click(createRuleButton);

    const useRuleBuilderButton = getByText('Use Rule Builder');
    const useSourceCodeButton = getByText('Use Source Code');

    expect(useRuleBuilderButton).toBeInTheDocument();
    expect(useSourceCodeButton).toBeInTheDocument();
  });
});
