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
import CartesianOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/CartesianOnClickPopoverDropdown';

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
  .visualization('bar')
  .build();

const clickPoint = {
  data: { originalName: 'GET', name: 'GET', marker: { color: '#ff0000' } },
  x: 'index.html',
  y: 408,
  pointIndex: 0,
  pointNumber: 0,
} as unknown as ClickPoint;

const renderDropdown = (props: Partial<React.ComponentProps<typeof CartesianOnClickPopoverDropdown>> = {}) => {
  const setFieldData = jest.fn();
  const setStep = jest.fn();

  render(
    <Wrapper>
      <CartesianOnClickPopoverDropdown
        clickPoint={clickPoint}
        config={config}
        setFieldData={setFieldData}
        setStep={setStep}
        {...props}
      />
    </Wrapper>,
  );

  return { setFieldData, setStep };
};

describe('CartesianOnClickPopoverDropdown', () => {
  it('renders the metric and grouping values derived from the clicked point', async () => {
    renderDropdown();

    await screen.findByText('Related values');

    expect(screen.getByText('Metric')).toBeInTheDocument();
    expect(screen.getByText('408')).toBeInTheDocument();
    expect(screen.getByText('count()')).toBeInTheDocument();

    expect(screen.getByText('Groupings')).toBeInTheDocument();
    expect(screen.getByText('index.html')).toBeInTheDocument();
    expect(screen.getByText('action')).toBeInTheDocument();
    expect(screen.getByText('GET')).toBeInTheDocument();
    expect(screen.getByText('source')).toBeInTheDocument();
  });

  it('calls setFieldData with the metric when the metric value is clicked', async () => {
    const { setFieldData } = renderDropdown();

    await userEvent.click(await screen.findByText('408'));

    expect(setFieldData).toHaveBeenCalledWith({ value: 408, field: 'count()', contexts: null });
  });

  it('calls setFieldData with the row pivot value when a grouping value is clicked', async () => {
    const { setFieldData } = renderDropdown();

    await userEvent.click(await screen.findByText('index.html'));

    expect(setFieldData).toHaveBeenCalledWith({ value: 'index.html', field: 'action', contexts: null });
  });

  it('does not render a back button by default', async () => {
    renderDropdown();

    await screen.findByText('Related values');

    expect(screen.queryByRole('button', { name: /back/i })).not.toBeInTheDocument();
  });

  it('goes back to the trace selection when the back button is clicked', async () => {
    const { setStep } = renderDropdown({ showBackButton: true });

    await userEvent.click(await screen.findByRole('button', { name: /back/i }));

    expect(setStep).toHaveBeenCalledWith('traces');
  });
});
