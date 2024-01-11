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

import CompatibilityCheckStep from 'components/datanode/migrations/CompatibilityCheckStep';
import { asMock } from 'helpers/mocking';
import useCompatibilityCheck from 'components/datanode/hooks/useCompatibilityCheck';

jest.mock('components/datanode/hooks/useCompatibilityCheck', () => jest.fn(() => ({
  data: {
    opensearch_version: '2.10.0',
    info: {
      nodes: [{
        indices: [{
          index_id: 'prlnhUp_TvSof9U-K3FZ9A',
          shards: [{ documents_count: 10, name: 'S0', primary: true, min_lucene_version: '9.7.0' }],
          index_name: '.opendistro_security',
          creation_date: '2023-11-17T09:57:36.511',
          index_version_created: '2.10.0',
        }],
      }],
      opensearch_data_location: '/home/tdvorak/bin/datanode/data',
    },
    compatibility_errors: [],
  },
  isFetching: false,
  isInitialLoading: false,
  error: undefined,
})));

describe('CompatibilityCheckStep', () => {
  it('should render CompatibilityCheckStep', async () => {
    render(<CompatibilityCheckStep onStepComplete={() => {}} />);

    await screen.findByRole('heading', {
      name: /your existing opensearch data can be migrated to datanode\./i,
    });
  });

  it('should render Compatibility error', async () => {
    asMock(useCompatibilityCheck).mockReturnValue({
      data: {
        opensearch_version: '2.10.0',
        info: null,
        compatibility_errors: ['org.graylog.shaded.opensearch2.org.apache.lucene.index.IndexFormatTooOldException: Format version is not supported (resource BufferedChecksumIndexInput(ByteBufferIndexInput(path="/index/segments_3"))): This index was initially created with Lucene 7.x while the current version is 9.7.0 and Lucene only supports reading the current and previous major versions. This version of Lucene only supports indexes created with release 8.0 and later by default.'],
      },
      refetch: () => {},
      isError: false,
      isInitialLoading: false,
      error: undefined,
    });

    render(<CompatibilityCheckStep onStepComplete={() => {}} />);

    await screen.findByRole('heading', {
      name: /your existing opensearch data cannot be migrated to datanode\./i,
    });
  });
});
