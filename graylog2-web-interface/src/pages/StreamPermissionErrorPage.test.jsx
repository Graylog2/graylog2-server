// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import { FetchError } from 'logic/rest/FetchProvider';

import StreamPermissionErrorPage from './StreamPermissionErrorPage';

jest.unmock('logic/rest/FetchProvider');

describe('StreamPermissionErrorPage', () => {
  it('displays fetch error', () => {
    suppressConsole(async () => {
      const response = { status: 403, body: { message: 'The request error message', streams: ['stream-1-id', 'stream-2-id'], type: 'MissingStreamPermission' } };
      const { getByText } = render(<StreamPermissionErrorPage error={new FetchError('The request error message', response)} />);

      expect(getByText('Missing Stream Permissions')).not.toBeNull();
      expect(getByText('You need permission to streams with the id: stream-1-id, stream-2-id')).not.toBeNull();
    });
  });
});
