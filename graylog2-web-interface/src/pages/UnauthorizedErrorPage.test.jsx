// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import { FetchError } from 'logic/rest/FetchProvider';

import UnauthorizedErrorPage from './UnauthorizedErrorPage';

jest.unmock('logic/rest/FetchProvider');

describe('UnauthorizedErrorPage', () => {
  it('displays fetch error', () => {
    suppressConsole(async () => {
      const { getByText } = render(<UnauthorizedErrorPage error={new FetchError('The request error message', new Error('The request error message'))} />);

      expect(getByText('Missing Permissions')).not.toBeNull();
      expect(getByText(/The request error message/)).not.toBeNull();
    });
  });
});
