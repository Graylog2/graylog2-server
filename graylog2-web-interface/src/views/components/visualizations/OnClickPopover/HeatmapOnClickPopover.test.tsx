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
import HeatmapOnClickPopover from 'views/components/visualizations/OnClickPopover/HeatmapOnClickPopover';

const fieldTypes: FieldTypes = { all: Immutable.List(), currentQuery: Immutable.List() };

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <FieldTypesContext.Provider value={fieldTypes}>
    <Popover opened withinPortal={false}>
      <Popover.Target>
        <button type="button">target</button>
      </Popover.Target>
      {children}
    </Popover>
  </FieldTypesContext.Provider>
);

const config = AggregationWidgetConfig.builder()
  .rowPivots([Pivot.createValues(['action'])])
  .columnPivots([Pivot.createValues(['source'])])
  .series([new Series('count()')])
  .visualization('heatmap')
  .build();

const clickPoint = {
  x: 'GET',
  y: 'index.html',
  z: 42,
} as unknown as ClickPoint;

const renderPopover = (props: Partial<React.ComponentProps<typeof HeatmapOnClickPopover>> = {}) => {
  const setFieldData = jest.fn();

  render(
    <Wrapper>
      <HeatmapOnClickPopover
        clickPoint={clickPoint}
        config={config}
        setFieldData={setFieldData}
        setStep={jest.fn()}
        {...props}
      />
    </Wrapper>,
  );

  return { setFieldData };
};

describe('HeatmapOnClickPopover', () => {
  it('renders the z value as title with the metric and axis values', async () => {
    renderPopover();

    // Title (h4) and metric value both render the z value.
    expect(await screen.findByRole('heading', { name: '42' })).toBeInTheDocument();
    expect(screen.getByText('Metric')).toBeInTheDocument();
    expect(screen.getByText('count()')).toBeInTheDocument();

    expect(screen.getByText('index.html')).toBeInTheDocument();
    expect(screen.getByText('action')).toBeInTheDocument();
    expect(screen.getByText('GET')).toBeInTheDocument();
    expect(screen.getByText('source')).toBeInTheDocument();
  });

  it('calls setFieldData with the row pivot value when it is clicked', async () => {
    const { setFieldData } = renderPopover();

    await userEvent.click(await screen.findByText('index.html'));

    expect(setFieldData).toHaveBeenCalledWith({ value: 'index.html', field: 'action', contexts: null });
  });

  it('calls setFieldData with the column pivot value when it is clicked', async () => {
    const { setFieldData } = renderPopover();

    await userEvent.click(await screen.findByText('GET'));

    expect(setFieldData).toHaveBeenCalledWith({ value: 'GET', field: 'source', contexts: null });
  });

  it('renders nothing when there is no clicked point', () => {
    const { container } = render(
      <Wrapper>
        <HeatmapOnClickPopover clickPoint={undefined} config={config} setFieldData={jest.fn()} setStep={jest.fn()} />
      </Wrapper>,
    );

    expect(container).not.toHaveTextContent('Metric');
  });
});
