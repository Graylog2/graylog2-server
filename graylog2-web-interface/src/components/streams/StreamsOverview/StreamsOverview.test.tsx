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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import { indexSets } from 'fixtures/indexSets';
import { asMock } from 'helpers/mocking';
import useStreams from 'components/streams/hooks/useStreams';
import { stream } from 'fixtures/streams';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';

import StreamsOverview from './StreamsOverview';

jest.mock('components/streams/hooks/useStreams');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

const attributes = [
  {
    id: 'title',
    title: 'Title',
    sortable: true,
  },
  {
    id: 'description',
    title: 'Description',
    sortable: true,
  },
];

const paginatedStreams = ({
  data: {
    pagination: {
      total: 1,
      page: 1,
      perPage: 5,
      count: 1,
    },
    elements: [stream],
    attributes,
  },
  refetch: () => {},
  isInitialLoading: false,
});

describe('StreamsOverview', () => {
  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({ data: layoutPreferences, isLoading: false });
  });

  it('should render empty', async () => {
    const emptyPaginatedStreams = ({
      data: {
        pagination: {
          total: 0,
          page: 1,
          perPage: 5,
          count: 0,
        },
        elements: [],
        attributes,
      },
      refetch: () => {},
      isInitialLoading: false,
    });
    asMock(useStreams).mockReturnValue(emptyPaginatedStreams);

    render(<StreamsOverview indexSets={indexSets} />);

    await screen.findByText('No streams have been found');
  });

  it('should render list', async () => {
    asMock(useStreams).mockReturnValue(paginatedStreams);

    render(<StreamsOverview indexSets={indexSets} />);

    await screen.findByText(stream.title);
    await screen.findByText(stream.description);
  });
});
