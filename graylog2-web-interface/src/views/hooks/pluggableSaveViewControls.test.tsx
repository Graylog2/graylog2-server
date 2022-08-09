import { QueryClientProvider, QueryClient } from 'react-query';
import { renderHook } from '@testing-library/react-hooks';
import * as React from 'react';

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'views/logic/usePluginEntities';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('views/logic/usePluginEntities');

describe('useSaveViewFormControls', () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  const wrapper = ({ children }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
  });

  it('should return save view form controls from plugin store', async () => {
    const saveViewFromControl = {
      component: () => <div>Pluggable component!</div>,
      id: 'example-plugin-component',
    };

    asMock(usePluginEntities).mockImplementation((entityKey) => ({
      'views.components.saveViewForm': [() => saveViewFromControl],
    }[entityKey]));

    const { result } = renderHook(() => useSaveViewFormControls(), { wrapper });

    expect(result.current).toEqual([saveViewFromControl]);
  });
});
