// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import { FetchError } from 'logic/rest/FetchProvider';
import { GlobalStylesContext } from 'contexts/GlobalStylesProvider';

import StreamPermissionErrorPage from './StreamPermissionErrorPage';

jest.unmock('logic/rest/FetchProvider');

describe('StreamPermissionErrorPage', () => {
  const renderSUT = (children) => {
    const addGlobalStyles = jest.fn();

    return render(
      <GlobalStylesContext.Provider value={{ addGlobalStyles }}>
        {children}
      </GlobalStylesContext.Provider>,
    );
  };

  it('displays fetch error', () => {
    const response = { status: 403, body: { message: 'The request error message', streams: ['stream-1-id', 'stream-2-id'], type: 'MissingStreamPermission' } };

    suppressConsole(async () => {
      const { getByText } = renderSUT(<StreamPermissionErrorPage error={new FetchError('The request error message', response)} />);

      expect(getByText('Missing Stream Permissions')).not.toBeNull();
      expect(getByText('You need permissions for streams with the id: stream-1-id, stream-2-id.')).not.toBeNull();
    });
  });
});
