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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import mockEntityShareState, { availableGrantees, availableCapabilities } from 'fixtures/entityShareState';

import GranteesSelector from './GranteesSelector';

describe('GranteesSelector', () => {
  it('shows validation error', async () => {
    const { getByText } = render(
      <GranteesSelector availableGrantees={availableGrantees}
                        availableCapabilities={availableCapabilities}
                        onSubmit={() => Promise.resolve(mockEntityShareState)}
                        granteesSelectRef={undefined} />,
    );

    const submitButton = getByText('Add Collaborator');

    fireEvent.click(submitButton);

    await waitFor(() => expect(getByText('The grantee is required.')).not.toBeNull());
  });
});
