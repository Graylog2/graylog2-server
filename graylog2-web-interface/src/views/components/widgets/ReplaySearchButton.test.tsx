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
import { asElement, render } from 'wrappedTestingLibrary';

import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import ReplaySearchButton from './ReplaySearchButton';

import DrilldownContext from '../contexts/DrilldownContext';

type OptionalOverrides = {
  streams?: Array<string>,
  query?: ElasticsearchQueryString,
  timerange?: TimeRange,
};

describe('ReplaySearchButton', () => {
  it('renders play button', () => {
    const { getByTitle } = render(<ReplaySearchButton />);

    expect(getByTitle('Replay search')).not.toBeNull();
  });

  describe('generates link', () => {
    const renderWithContext = ({ query, timerange, streams }: OptionalOverrides = {}) => {
      const { getByTitle } = render((
        <DrilldownContext.Consumer>
          {(context) => (
            <DrilldownContext.Provider value={{
              query: query || context.query,
              timerange: timerange || context.timerange,
              streams: streams || context.streams,
            }}>
              <ReplaySearchButton />
            </DrilldownContext.Provider>
          )}
        </DrilldownContext.Consumer>
      ));

      return asElement(getByTitle('Replay search'), HTMLAnchorElement);
    };

    it('from default drilldown context', () => {
      const { getByTitle } = render(<ReplaySearchButton />);
      const button = asElement(getByTitle('Replay search'), HTMLAnchorElement);

      expect(button.href).toEqual('http://localhost/search?rangetype=relative&from=300');
    });

    it('opening in a new page', () => {
      const button = renderWithContext();

      expect(button.target).toEqual('_blank');
      expect(button.rel).toEqual('noopener noreferrer');
    });

    it('including query string', () => {
      const button = renderWithContext({ query: createElasticsearchQueryString('_exists_:nf_version') });

      expect(button.href).toContain('q=_exists_%3Anf_version');
    });

    it('including timerange', () => {
      const button = renderWithContext({
        timerange: {
          type: 'absolute',
          from: '2020-01-10T13:23:42.000Z',
          to: '2020-01-10T14:23:42.000Z',
        },
      });

      expect(button.href).toContain('rangetype=absolute&from=2020-01-10T13%3A23%3A42.000Z&to=2020-01-10T14%3A23%3A42.000Z');
    });

    it('including streams', () => {
      const button = renderWithContext({ streams: ['stream1', 'stream2', 'someotherstream'] });

      expect(button.href).toContain('streams=stream1%2Cstream2%2Csomeotherstream');
    });
  });
});
