// @flow strict
import * as React from 'react';
import { render, cleanup } from 'wrappedTestingLibrary';

import SearchExecutionErrors from './SearchExecutionErrors';

describe('SearchExecutionError', () => {
  afterEach(cleanup);
  it('displays common errors', () => {
    const commonError = { additional: { body: { message: 'Common error message' } } };
    const { getByText } = render(<SearchExecutionErrors errors={[commonError]} />);
    expect(getByText('Common error message')).not.toBeNull();
  });
  it('displays provided stream ids', () => {
    const commonError = { additional: { body: { message: 'Common error message', streams: ['stream-id-1'] } } };
    const { getByText } = render(<SearchExecutionErrors errors={[commonError]} />);
    expect(getByText(/stream-id-1/)).not.toBeNull();
  });
  it('displays error, with uncommen format', () => {
    const uncommonError = { message: 'Uncommon error message' };
    const { getByText } = render(<SearchExecutionErrors errors={[uncommonError]} />);
    expect(getByText(/Uncommon error message/)).not.toBeNull();
  });
});
