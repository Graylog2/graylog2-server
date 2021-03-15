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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import AggregationActionSelect from './AggregationActionSelect';
import { AggregationAction } from './AggregationWizard';

const aggregationActions: Array<AggregationAction> = [
  {
    title: 'Metric',
    key: 'metric',
    isConfigured: false,
    multipleUse: true,
    onCreate: () => {},
    component: () => <div />,
  },
  {
    title: 'Sort',
    key: 'sort',
    multipleUse: false,
    isConfigured: true,
    onCreate: () => {},
    component: () => <div />,
  },
];

describe('AggregationActionSelect', () => {
  it('should select an aggregation action', async () => {
    const onActionCreateMock = jest.fn();

    render(<AggregationActionSelect onActionCreate={onActionCreateMock}
                                    aggregationActions={aggregationActions} />);

    const aggregationActionSelect = screen.getByLabelText('Add an Action');

    await selectEvent.openMenu(aggregationActionSelect);
    await selectEvent.select(aggregationActionSelect, 'Metric');

    expect(onActionCreateMock).toHaveBeenCalledTimes(1);
    expect(onActionCreateMock).toHaveBeenCalledWith('metric');
  });

  it('should not list already configured aggregation actions which can not be configured multiple times', async () => {
    render(<AggregationActionSelect onActionCreate={() => {}}
                                    aggregationActions={aggregationActions} />);

    const aggregationActionSelect = screen.getByLabelText('Add an Action');

    await selectEvent.openMenu(aggregationActionSelect);

    expect(screen.queryByText('Sort')).not.toBeInTheDocument();
    expect(screen.getByText('Metric')).toBeInTheDocument();
  });
});
