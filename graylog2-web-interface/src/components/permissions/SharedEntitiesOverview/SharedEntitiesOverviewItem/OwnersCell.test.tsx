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
