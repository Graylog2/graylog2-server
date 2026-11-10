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
import PieOnClickPopoverDropdown from 'views/components/visualizations/OnClickPopover/PieOnClickPopoverDropdown';

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
  .columnPivots([])
  .series([new Series('count()')])
  .visualization('pie')
  .build();

const clickPoint = {
  data: { originalName: 'count()', name: 'count()', originalLabels: ['index.html'], marker: { colors: ['#00ff00'] } },
  value: 408,
  text: 'Pie slice',
  pointNumber: 0,
} as unknown as ClickPoint;

const renderDropdown = (props: Partial<React.ComponentProps<typeof PieOnClickPopoverDropdown>> = {}) => {
  const setFieldData = jest.fn();

  render(
    <Wrapper>
      <PieOnClickPopoverDropdown
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

describe('PieOnClickPopoverDropdown', () => {
  it('renders the slice title with the metric and row pivot values', async () => {
    renderDropdown();

    await screen.findByText('Pie slice');

    expect(screen.getByText('Metric')).toBeInTheDocument();
    expect(screen.getByText('408')).toBeInTheDocument();
    expect(screen.getByText('count()')).toBeInTheDocument();

    expect(screen.getByText('index.html')).toBeInTheDocument();
    expect(screen.getByText('action')).toBeInTheDocument();
  });

  it('calls setFieldData with the row pivot value when it is clicked', async () => {
    const { setFieldData } = renderDropdown();

    await userEvent.click(await screen.findByText('index.html'));

    expect(setFieldData).toHaveBeenCalledWith({ value: 'index.html', field: 'action', contexts: null });
  });

  it('uses the formatted percentage as title when a percentage is present', async () => {
    const clickPointWithPercent = { ...clickPoint, percent: 0.42 } as unknown as ClickPoint;
    render(
      <Wrapper>
        <PieOnClickPopoverDropdown
          clickPoint={clickPointWithPercent}
          config={config}
          setFieldData={jest.fn()}
          setStep={jest.fn()}
        />
      </Wrapper>,
    );

    // The percentage branch formats clickPoint.percent instead of falling back to clickPoint.text.
    expect(await screen.findByRole('heading', { name: /42/ })).toBeInTheDocument();
    expect(screen.queryByText('Pie slice')).not.toBeInTheDocument();
  });
});
