import * as React from 'react';
import { act, render, screen, waitFor } from 'wrappedTestingLibrary';

import type { SearchBarFormValues } from 'views/Constants';
import { SearchQueryStrings } from '@graylog/server-api';
import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';

import useSearchBarSubmit from './useSearchBarSubmit';

jest.mock('@graylog/server-api', () => ({
  SearchQueryStrings: {
    queryStringUsed: jest.fn(),
  },
}));

type Props = {
  onSubmit: (v: SearchBarFormValues) => Promise<void>,
  values: SearchBarFormValues,
}

const initialValues: SearchBarFormValues = {
  queryString: 'action:login',
  timerange: { type: 'relative', range: 300 },
  streams: [],
};

const Wrapper = ({ onSubmit, values }: Props) => {
  const { onSubmit: _onSubmit, enableReinitialize } = useSearchBarSubmit(initialValues, onSubmit);

  return <button type="button" onClick={() => _onSubmit(values)}>{enableReinitialize ? 'submit' : 'busy'}</button>;
};

const clickSubmit = async () => {
  (await screen.findByRole('button', { name: 'submit' })).click();
};

describe('useSearchBarSubmit', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('records trimmed query strings through API', async () => {
    const onSubmit = jest.fn(async () => {});
    const values: SearchBarFormValues = {
      queryString: ' http_method:POST ',
      streams: [],
      timerange: { type: 'relative', range: 60 },
    };
    render(<Wrapper onSubmit={onSubmit} values={values} />);

    await clickSubmit();

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(values);
    });

    await waitFor(() => {
      expect(SearchQueryStrings.queryStringUsed).toHaveBeenCalledWith({ query_string: 'http_method:POST' });
    });
  });

  it('does not record empty query strings', async () => {
    const onSubmit = jest.fn(async () => {});
    const values: SearchBarFormValues = {
      queryString: '  ',
      streams: [],
      timerange: { type: 'relative', range: 60 },
    };
    render(<Wrapper onSubmit={onSubmit} values={values} />);

    await act(async () => {
      await clickSubmit();
    });

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(values);
    });

    expect(SearchQueryStrings.queryStringUsed).not.toHaveBeenCalled();
  });

  it('does not record identical query strings', async () => {
    const onSubmit = jest.fn(async () => {});
    const values: SearchBarFormValues = {
      queryString: ' action:login ',
      streams: [],
      timerange: { type: 'relative', range: 60 },
    };
    render(<Wrapper onSubmit={onSubmit} values={values} />);

    await act(async () => {
      await clickSubmit();
    });

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(values);
    });

    expect(SearchQueryStrings.queryStringUsed).not.toHaveBeenCalled();
  });

  it('still submits even if query string recording fails', async () => {
    const onSubmit = jest.fn(async () => {});
    const values: SearchBarFormValues = {
      queryString: ' http_method:POST ',
      streams: [],
      timerange: { type: 'relative', range: 60 },
    };
    asMock(SearchQueryStrings.queryStringUsed).mockRejectedValue(new Error('Boom!'));
    render(<Wrapper onSubmit={onSubmit} values={values} />);

    await suppressConsole(async () => {
      await clickSubmit();
    });

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(values);
    });
  });
});
