// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';

import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
import DrilldownContext from '../contexts/DrilldownContext';
import ReplaySearchButton from './ReplaySearchButton';
import { asElement } from '../../../../test/wrappedTestingLibrary';

type OptionalOverrides = {
  streams?: Array<string>,
  query?: ElasticsearchQueryString,
  timerange?: TimeRange,
};

describe('ReplaySearchButton', () => {
  afterEach(cleanup);
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

      expect(button.href).toEqual('http://localhost/search?rangetype=relative&relative=300');
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
