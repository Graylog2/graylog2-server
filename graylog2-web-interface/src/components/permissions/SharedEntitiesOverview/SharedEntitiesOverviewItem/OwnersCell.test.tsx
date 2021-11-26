import * as React from 'react';
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';

import Grantee from 'logic/permissions/Grantee';

import OwnersCell from './OwnersCell';

const everyone = Grantee.builder()
  .type('global')
  .id('grn::::global:everyone')
  .title('grn::::global:everyone')
  .build();

const SUT = (props: React.ComponentProps<typeof OwnersCell>) => (
  <table>
    <tbody>
      <tr><OwnersCell {...props} /></tr>
    </tbody>
  </table>
);

describe('OwnersCell', () => {
  it('renders global share as Everyone', async () => {
    render(<SUT owners={Immutable.List([everyone])} />);
    await screen.findByText('Everyone');
  });
});
