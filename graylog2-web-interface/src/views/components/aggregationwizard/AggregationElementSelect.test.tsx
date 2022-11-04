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
import userEvent from '@testing-library/user-event';

import AggregationElementSelect from './AggregationElementSelect';
import type { AggregationElement } from './AggregationElementType';

const aggregationElements = [
  {
    title: 'Metric',
    key: 'metrics',
    order: 1,
    allowCreate: () => true,
    component: () => <div />,
    isEmpty: () => true,
  } as AggregationElement<'metrics'>,
  {
    title: 'Sort',
    key: 'sort',
    order: 1,
    allowCreate: () => false,
    component: () => <div />,
    isEmpty: () => true,
  } as AggregationElement<'sort'>,
];

describe('AggregationElementSelect', () => {
  it('should select an aggregation element', async () => {
    const onSelectMock = jest.fn();

    render(<AggregationElementSelect onSelect={onSelectMock}
                                     formValues={{ metrics: [] }}
                                     aggregationElements={aggregationElements} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Add' }));

    await userEvent.click(await screen.findByRole('menuitem', { name: 'Metric' }));

    expect(onSelectMock).toHaveBeenCalledTimes(1);
    expect(onSelectMock).toHaveBeenCalledWith('metrics');
  });

  it('should not list already configured aggregation elements which can not be configured multiple times', async () => {
    render(<AggregationElementSelect onSelect={() => {}}
                                     formValues={{ metrics: [] }}
                                     aggregationElements={aggregationElements} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Add' }));

    expect(screen.queryByText('Sort')).not.toBeInTheDocument();
    expect(screen.getByText('Metric')).toBeInTheDocument();
  });
});
