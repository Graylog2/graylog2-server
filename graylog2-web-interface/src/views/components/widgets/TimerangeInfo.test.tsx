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

import Widget from 'views/logic/widgets/Widget';

import TimerangeInfo from './TimerangeInfo';

describe('TimerangeInfo', () => {
  const widget = Widget.empty();

  it('should display a relative timerange', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 3000 }).build();
    render(<TimerangeInfo widget={relativeWidget} />);

    expect(screen.getByText('an hour ago - Now')).toBeInTheDocument();
  });

  it('should display a All Time', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 0 }).build();
    render(<TimerangeInfo widget={relativeWidget} />);

    expect(screen.getByText('All Time')).toBeInTheDocument();
  });

  it('should display a absolute timerange', () => {
    const absoluteWidget = widget.toBuilder()
      .timerange({ type: 'absolute', from: '2021-03-27T14:32:31.894Z', to: '2021-04-26T14:32:48.000Z' })
      .build();
    render(<TimerangeInfo widget={absoluteWidget} />);

    expect(screen.getByText('2021-03-27T14:32:31.894Z - 2021-04-26T14:32:48.000Z')).toBeInTheDocument();
  });

  it('should display a keyword timerange', () => {
    const keywordWidget = widget.toBuilder()
      .timerange({ type: 'keyword', keyword: '5 minutes ago' })
      .build();
    render(<TimerangeInfo widget={keywordWidget} />);

    expect(screen.getByText('5 minutes ago')).toBeInTheDocument();
  });
});
