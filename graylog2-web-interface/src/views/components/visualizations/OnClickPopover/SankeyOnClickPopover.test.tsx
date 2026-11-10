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
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import Popover from 'components/common/Popover';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import SankeyOnClickPopover from 'views/components/visualizations/OnClickPopover/SankeyOnClickPopover';
import { ActionContext } from 'views/logic/ActionContext';
import type { ActionContexts } from 'views/types';

// The action menu itself is a separate, independently tested component. Mock it so these tests
// focus on how SankeyOnClickPopover turns a clicked node/link into selectable values.
jest.mock('views/components/actions/ActionDropdown', () => ({
  __esModule: true,
  default: ({ handlerArgs }: { handlerArgs?: { contexts?: { valuePath?: unknown; valuePathOperator?: string } } }) => (
    <div
      data-testid="action-dropdown"
      data-value-path={JSON.stringify(handlerArgs?.contexts?.valuePath ?? null)}
      data-value-path-operator={handlerArgs?.contexts?.valuePathOperator ?? ''}>
      actions
    </div>
  ),
}));

const fieldTypes: FieldTypes = { all: Immutable.List(), currentQuery: Immutable.List() };

// `hasMultipleValueForActions` reads the visualization from the surrounding widget context.
const actionContext = { widget: { config: { visualization: 'sankey' } } } as unknown as ActionContexts;

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <FieldTypesContext.Provider value={fieldTypes}>
    <ActionContext.Provider value={actionContext}>
      <Popover opened withinPortal={false}>
        <Popover.Target>
          <button type="button">target</button>
        </Popover.Target>
        {children}
      </Popover>
    </ActionContext.Provider>
  </FieldTypesContext.Provider>
);

const config = AggregationWidgetConfig.builder()
  .rowPivots([])
  .columnPivots([])
  .series([new Series('count()')])
  .visualization('sankey')
  .build();

const linkClickPoint = {
  source: { label: 'GET', customdata: { field: 'action', value: 'GET' } },
  target: { label: 'index.html', customdata: { field: 'page', value: 'index.html' } },
  value: 408,
} as unknown as ClickPoint;

const nodeClickPoint = {
  customdata: { field: 'action', value: 'GET' },
  label: 'GET',
} as unknown as ClickPoint;

// Advanced field types resolve an id to a name (the node label), so the label differs from the raw value.
const linkClickPointWithResolvedIds = {
  source: { label: 'Stream A', customdata: { field: 'streams', value: 'id-1' } },
  target: { label: 'Stream B', customdata: { field: 'streams', value: 'id-2' } },
  value: 408,
} as unknown as ClickPoint;

const renderPopover = (clickPoint: ClickPoint, onPopoverClose = jest.fn()) => {
  render(
    <Wrapper>
      <SankeyOnClickPopover clickPoint={clickPoint} config={config} onPopoverClose={onPopoverClose} />
    </Wrapper>,
  );

  return { onPopoverClose };
};

describe('SankeyOnClickPopover', () => {
  describe('for a link', () => {
    it('renders the connection title with source, target and metric values', async () => {
      renderPopover(linkClickPoint);

      expect(await screen.findByRole('heading', { name: 'GET → index.html' })).toBeInTheDocument();
      expect(screen.getByText('GET')).toBeInTheDocument();
      expect(screen.getByText('action')).toBeInTheDocument();
      expect(screen.getByText('index.html')).toBeInTheDocument();
      expect(screen.getByText('page')).toBeInTheDocument();
      expect(screen.getByText('408')).toBeInTheDocument();
      expect(screen.getByText('count()')).toBeInTheDocument();

      // Multiple selectable values, so it does not jump straight to the action menu.
      expect(screen.queryByTestId('action-dropdown')).not.toBeInTheDocument();
    });

    it('shows the combined grouping values, not the metric, after selecting the combined groupings', async () => {
      renderPopover(linkClickPoint);

      await userEvent.click(await screen.findByText('index.html-GET'));

      expect(await screen.findByTestId('action-dropdown')).toBeInTheDocument();

      // The title shows the combined grouping values...
      expect(screen.getByText('index.html-GET')).toBeInTheDocument();

      // ...not the metric and its value.
      expect(screen.queryByText('count()')).not.toBeInTheDocument();
      expect(screen.queryByText('408')).not.toBeInTheDocument();
    });

    it('does not tag the sankey combined groupings option with the EDGE operator', async () => {
      renderPopover(linkClickPoint);

      await userEvent.click(await screen.findByText('index.html-GET'));

      const dropdown = await screen.findByTestId('action-dropdown');

      expect(dropdown).toHaveAttribute('data-value-path-operator', '');
    });

    it('shows resolved node labels rather than raw ids in the groupings dialog', async () => {
      renderPopover(linkClickPointWithResolvedIds);

      // Individual grouping rows show the resolved names.
      expect(await screen.findByText('Stream A')).toBeInTheDocument();
      expect(screen.getByText('Stream B')).toBeInTheDocument();

      // The combined "apply all groupings" row also shows the resolved names, not the raw ids.
      expect(screen.getByText('Stream B-Stream A')).toBeInTheDocument();
      expect(screen.queryByText(/id-1/)).not.toBeInTheDocument();
      expect(screen.queryByText(/id-2/)).not.toBeInTheDocument();
    });

    it('shows the action menu after selecting a value and returns on back', async () => {
      renderPopover(linkClickPoint);

      await userEvent.click(await screen.findByText('GET'));

      expect(await screen.findByTestId('action-dropdown')).toBeInTheDocument();
      expect(screen.getByText(/action =/)).toBeInTheDocument();

      await userEvent.click(screen.getByRole('button', { name: /back/i }));

      expect(await screen.findByRole('heading', { name: 'GET → index.html' })).toBeInTheDocument();
      expect(screen.queryByTestId('action-dropdown')).not.toBeInTheDocument();
    });
  });

  describe('for a node', () => {
    it('jumps straight to the action menu for its single value', async () => {
      renderPopover(nodeClickPoint);

      expect(await screen.findByTestId('action-dropdown')).toBeInTheDocument();
      expect(screen.getByText(/action =/)).toBeInTheDocument();

      // Single value, so no back button and no value-selection step.
      expect(screen.queryByRole('button', { name: /back/i })).not.toBeInTheDocument();
    });
  });

  describe('for a network graph node', () => {
    const networkConfig = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source'])])
      .columnPivots([Pivot.createValues(['target'])])
      .series([new Series('count()')])
      .visualization('network')
      .build();
    const networkActionContext = { widget: { config: { visualization: 'network' } } } as unknown as ActionContexts;
    const networkNode = { customdata: { field: 'source', value: 'a' }, label: 'a' } as unknown as ClickPoint;

    it('targets the node value across all configured groupings, combined with OR', async () => {
      render(
        <FieldTypesContext.Provider value={fieldTypes}>
          <ActionContext.Provider value={networkActionContext}>
            <Popover opened withinPortal={false}>
              <Popover.Target>
                <button type="button">target</button>
              </Popover.Target>
              <SankeyOnClickPopover clickPoint={networkNode} config={networkConfig} onPopoverClose={jest.fn()} />
            </Popover>
          </ActionContext.Provider>
        </FieldTypesContext.Provider>,
      );

      const dropdown = await screen.findByTestId('action-dropdown');

      expect(dropdown).toHaveAttribute('data-value-path-operator', 'OR');
      expect(JSON.parse(dropdown.getAttribute('data-value-path'))).toEqual([{ source: 'a' }, { target: 'a' }]);
    });
  });

  describe('for a network graph edge', () => {
    const networkConfig = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source'])])
      .columnPivots([Pivot.createValues(['target'])])
      .series([new Series('count()')])
      .visualization('network')
      .build();
    const networkActionContext = { widget: { config: { visualization: 'network' } } } as unknown as ActionContexts;

    it('tags the combined groupings option with the EDGE operator', async () => {
      render(
        <FieldTypesContext.Provider value={fieldTypes}>
          <ActionContext.Provider value={networkActionContext}>
            <Popover opened withinPortal={false}>
              <Popover.Target>
                <button type="button">target</button>
              </Popover.Target>
              <SankeyOnClickPopover clickPoint={linkClickPoint} config={networkConfig} onPopoverClose={jest.fn()} />
            </Popover>
          </ActionContext.Provider>
        </FieldTypesContext.Provider>,
      );

      await userEvent.click(await screen.findByText('index.html-GET'));

      const dropdown = await screen.findByTestId('action-dropdown');

      expect(dropdown).toHaveAttribute('data-value-path-operator', 'EDGE');
      expect(JSON.parse(dropdown.getAttribute('data-value-path'))).toEqual([{ page: 'index.html' }, { action: 'GET' }]);
    });
  });

  it('renders nothing when there is no clicked point', () => {
    const { container } = render(
      <Wrapper>
        <SankeyOnClickPopover clickPoint={undefined} config={config} onPopoverClose={jest.fn()} />
      </Wrapper>,
    );

    expect(container).not.toHaveTextContent('count()');
  });
});
