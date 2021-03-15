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

import AggregationElementSelect from './AggregationElementSelect';
import { AggregationElement } from './AggregationWizard';

const aggregationElements: Array<AggregationElement> = [
  {
    title: 'Metric',
    key: 'metric',
    order: 1,
    isConfigured: false,
    multipleUse: true,
    onCreate: () => {},
    component: () => <div />,
  },
  {
    title: 'Sort',
    key: 'sort',
    order: 1,
    multipleUse: false,
    isConfigured: true,
    onCreate: () => {},
    component: () => <div />,
  },
];

describe('AggregationElementSelect', () => {
  it('should select an aggregation element', async () => {
    const onElementCreateMock = jest.fn();

    render(<AggregationElementSelect onElementCreate={onElementCreateMock}
                                     aggregationElements={aggregationElements} />);

    const aggregationElementSelect = screen.getByLabelText('Add an Element');

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Metric');

    expect(onElementCreateMock).toHaveBeenCalledTimes(1);
    expect(onElementCreateMock).toHaveBeenCalledWith('metric');
  });

  it('should not list already configured aggregation elements which can not be configured multiple times', async () => {
    render(<AggregationElementSelect onElementCreate={() => {}}
                                     aggregationElements={aggregationElements} />);

    const aggregationElementSelect = screen.getByLabelText('Add an Element');

    await selectEvent.openMenu(aggregationElementSelect);

    expect(screen.queryByText('Sort')).not.toBeInTheDocument();
    expect(screen.getByText('Metric')).toBeInTheDocument();
  });
});
