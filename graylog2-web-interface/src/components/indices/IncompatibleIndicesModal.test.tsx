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

import asMock from 'helpers/mocking/AsMock';

import IncompatibleIndicesModal from './IncompatibleIndicesModal';

jest.mock('components/indices/incompatible-indices/IncompatibleIndicesTable', () => ({
  __esModule: true,
  default: jest.fn(() => <div>incompatible-indices-table</div>),
}));

describe('IncompatibleIndicesModal', () => {
  it('shows the incompatible indices table with local pagination state', async () => {
    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(await screen.findByText('incompatible-indices-table')).toBeInTheDocument();
    expect(screen.getByText(/need to be archived, deleted or reindexed/i)).toBeInTheDocument();

    const { default: IncompatibleIndicesTable } = jest.requireMock(
      'components/indices/incompatible-indices/IncompatibleIndicesTable',
    );
    expect(asMock(IncompatibleIndicesTable).mock.calls[0][0]).toEqual(
      expect.objectContaining({ withoutURLParams: true }),
    );
  });
});
