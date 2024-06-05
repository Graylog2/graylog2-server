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
import { asElement, render, screen } from 'wrappedTestingLibrary';

import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import ReplaySearchButton from './ReplaySearchButton';

type OptionalOverrides = {
  streams?: Array<string>,
  query?: ElasticsearchQueryString,
  timerange?: TimeRange,
};

describe('ReplaySearchButton', () => {
  it('renders play button', async () => {
    render(<ReplaySearchButton />);

    await screen.findByRole('link', { name: /replay search/i });
  });

  describe('generates link', () => {
    const renderWithContext = async ({ query, timerange, streams }: OptionalOverrides = {}) => {
      render(<ReplaySearchButton queryString={query?.query_string}
                                 timerange={timerange}
                                 streams={streams} />);

      return asElement(await screen.findByRole('link', { name: /replay search/i }), HTMLAnchorElement);
    };

    it('opening in a new page', async () => {
      const button = await renderWithContext();

      expect(button.target).toEqual('_blank');
      expect(button.rel).toEqual('noopener noreferrer');
    });

    it('including query string', async () => {
      const button = await renderWithContext({ query: createElasticsearchQueryString('_exists_:nf_version') });

      expect(button.href).toContain('q=_exists_%3Anf_version');
    });

    it('including timerange', async () => {
      const button = await renderWithContext({
        timerange: {
          type: 'absolute',
          from: '2020-01-10T13:23:42.000Z',
          to: '2020-01-10T14:23:42.000Z',
        },
      });

      expect(button.href).toContain('rangetype=absolute&from=2020-01-10T13%3A23%3A42.000Z&to=2020-01-10T14%3A23%3A42.000Z');
    });

    it('including streams', async () => {
      const button = await renderWithContext({ streams: ['stream1', 'stream2', 'someotherstream'] });

      expect(button.href).toContain('streams=stream1%2Cstream2%2Csomeotherstream');
    });
  });
});
