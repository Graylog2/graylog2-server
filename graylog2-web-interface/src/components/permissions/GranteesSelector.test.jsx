// @flow strict
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
