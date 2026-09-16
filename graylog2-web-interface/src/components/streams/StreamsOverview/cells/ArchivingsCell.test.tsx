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

import { asMock } from 'helpers/mocking';
import useExcludedArchiveStreams from 'components/streams/hooks/useExcludedArchiveStreams';

import ArchivingsCell from './ArchivingsCell';

jest.mock('components/streams/hooks/useExcludedArchiveStreams');

const stream = { id: 'stream-1', title: 'Test Stream', is_default: false, is_editable: true, index_set_id: 'is-1' } as any;
const archivingIndexSet = { id: 'is-1', data_tiering: { archive_before_deletion: true } } as any;
const nonArchivingIndexSet = { id: 'is-1', data_tiering: { archive_before_deletion: false } } as any;

describe('ArchivingsCell (Streams)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useExcludedArchiveStreams).mockReturnValue([]);
  });

  it('shows archiving enabled for the default stream when its index set archives', () => {
    render(<ArchivingsCell stream={{ ...stream, is_default: true }} indexSets={[archivingIndexSet]} />);

    expect(screen.getByTitle('Yes')).toBeInTheDocument();
  });

  it('shows archiving enabled for a non-editable stream when its index set archives', () => {
    render(<ArchivingsCell stream={{ ...stream, is_editable: false }} indexSets={[archivingIndexSet]} />);

    expect(screen.getByTitle('Yes')).toBeInTheDocument();
  });

  it('renders nothing when the index set does not archive', () => {
    render(<ArchivingsCell stream={stream} indexSets={[nonArchivingIndexSet]} />);

    expect(screen.queryByTitle('Yes')).not.toBeInTheDocument();
  });

  it('shows archiving enabled when the index set archives and the stream is not excluded', () => {
    render(<ArchivingsCell stream={stream} indexSets={[archivingIndexSet]} />);

    expect(screen.getByTitle('Yes')).toBeInTheDocument();
  });

  it('renders nothing when the index set archives but the stream is excluded', () => {
    asMock(useExcludedArchiveStreams).mockReturnValue(['stream-1']);

    render(<ArchivingsCell stream={stream} indexSets={[archivingIndexSet]} />);

    expect(screen.queryByTitle('Yes')).not.toBeInTheDocument();
  });

  it('renders nothing for the default stream when it is excluded from archiving', () => {
    asMock(useExcludedArchiveStreams).mockReturnValue(['stream-1']);

    render(<ArchivingsCell stream={{ ...stream, is_default: true }} indexSets={[archivingIndexSet]} />);

    expect(screen.queryByTitle('Yes')).not.toBeInTheDocument();
  });
});
