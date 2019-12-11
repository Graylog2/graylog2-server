import React from 'react';
import { render, fireEvent, wait, waitForElementToBeRemoved, cleanup } from '@testing-library/react';
import { StoreMock as MockStore } from 'helpers/mocking';

import Store from 'logic/local-storage/Store';
import SearchActions from 'views/actions/SearchActions';

import MigrateFieldCharts from '../MigrateFieldCharts';
import { mockFieldCharts, viewState as mockViewState } from './MigrateFieldCharts.fixtures';

const mockStoreGet = (fieldChart = {}, migrated = false) => (key) => {
  switch (key) {
    case 'pinned-field-charts-migrated':
      return migrated;
    case 'pinned-field-charts':
      return mockFieldCharts(fieldChart);
    default:
      return undefined;
  }
};

jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

jest.mock('views/stores/ViewStatesStore', () => ({
  ViewStatesActions: {
    update: jest.fn(() => Promise.resolve()),
  },
}));


jest.mock('views/actions/SearchActions', () => ({
  executeWithCurrentState: jest.fn(() => Promise.resolve()),
}));


jest.mock('views/stores/CurrentViewStateStore', () => ({
  CurrentViewStateStore: MockStore(
    ['getInitialState', () => {
      return {
        state: mockViewState(),
        activeQuery: 'active-query-id',
      };
    },
    ],
  ),
}));

jest.mock('views/logic/Widgets', () => ({
  widgetDefinition: () => ({
    defaultHeight: 4,
    defaultWidth: 4,
  }),
}));


describe('MigrateFieldCharts', () => {
  afterEach(cleanup);

  it('should be visible if migration never got executed', async () => {
    Store.get.mockImplementation(mockStoreGet());
    const { getByText } = render(<MigrateFieldCharts />);

    expect(getByText('Migrate existing search page charts')).not.toBeNull();
  });

  it('should not be visible if migration already got executed', async () => {
    Store.get.mockImplementation(mockStoreGet(undefined, true));
    const { queryByText } = render(<MigrateFieldCharts />);
    expect(queryByText('Migrate existing search page charts')).toBeNull();
  });

  describe('migration should', () => {
    it('should execute search, when finished', async () => {
      Store.get.mockImplementation(mockStoreGet());

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      await wait(() => expect(SearchActions.executeWithCurrentState).toHaveBeenCalled());
    });

    it('should hide alert, when finished', async () => {
      Store.get.mockImplementation(mockStoreGet());

      const { queryByText } = render(<MigrateFieldCharts />);
      const migrateButton = queryByText('Migrate');
      fireEvent.click(migrateButton);

      await waitForElementToBeRemoved(() => queryByText('Migrate existing search page charts'));
    });

    it('create row pivot with interval unit months, if field chart interval is quarter', async () => {
      Store.get.mockImplementation(mockStoreGet({ interval: 'quarter' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });

    it('create row pivot with interval unit months, if field chart interval is month', async () => {
      Store.get.mockImplementation(mockStoreGet({ interval: 'month' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });

    it('set visualization to scatter, if field chart visualization is scatter', async () => {
      Store.get.mockImplementation(mockStoreGet({ visualization: 'scatterplot' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });

    it('set visualization to line, if field chart visualization is line', async () => {
      Store.get.mockImplementation(mockStoreGet({ visualization: 'line' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });

    it('create visualization config with interpolation spline, if field chart interpolation is bundle', async () => {
      Store.get.mockImplementation(mockStoreGet({ interpolation: 'bundle' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });

    it('create visualization config with interpolation linear, if field chart interpolation is linear', async () => {
      Store.get.mockImplementation(mockStoreGet({ interval: 'month' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });

    it('create area visualization config, if field chart visualization is area', async () => {
      Store.get.mockImplementation(mockStoreGet({ interval: 'month' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });

    it('create line visualization config, if field chart visualization is line', async () => {
      Store.get.mockImplementation(mockStoreGet({ interval: 'month' }));

      const { getByText } = render(<MigrateFieldCharts />);
      const migrateButton = getByText('Migrate');
      fireEvent.click(migrateButton);

      // await wait(() => expect(ViewStatesActions.update).toMatchSnapshot());
    });
  });


  // should not be visible after migration
});
