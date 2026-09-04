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
import { render, screen, within } from 'wrappedTestingLibrary';

import Trend from './Trend';

const renderTrend = ({ current = 42, previous = 42 }: Partial<React.ComponentProps<typeof Trend>> = {}) =>
  render(<Trend current={current} previous={previous} />);

const findTrend = async () => {
  const trend = await screen.findByTestId('trend-value');

  return trend.innerHTML;
};

describe('Trend', () => {
  it('shows absolute delta', async () => {
    renderTrend({ previous: 23 });

    expect(await findTrend()).toMatch(/\+19/);
    expect(await screen.findByTestId('trend-value')).toHaveAccessibleName(
      'Trend: +19 (+82.6%) compared to previous value of 23',
    );
  });

  it('shows relative delta as percentage', async () => {
    renderTrend({ previous: 23 });

    expect(await findTrend()).toMatch(/\+82.6%/);
  });

  it('shows absolute delta if values are equal', async () => {
    renderTrend();

    expect(await findTrend()).toMatch(/^0 \//);
    expect(await screen.findByTestId('trend-value')).toHaveAccessibleName(
      'Trend: 0 (0.0%) compared to previous value of 42',
    );
  });

  it('shows relative delta as percentage if values are equal', async () => {
    renderTrend();

    expect(await findTrend()).toMatch(/0\.0%/);
  });

  it('shows negative absolute delta', async () => {
    renderTrend({ current: 23 });

    expect(await findTrend()).toMatch(/-19/);
    expect(await screen.findByTestId('trend-value')).toHaveAccessibleName(
      'Trend: -19 (-45.2%) compared to previous value of 42',
    );
  });

  it('shows negative relative delta as percentage', async () => {
    renderTrend({ current: 23 });

    expect(await findTrend()).toMatch(/-45.2%/);
  });

  it('shows adequate results if previous value is 0', async () => {
    renderTrend({ current: 23, previous: 0 });

    expect(await findTrend()).toMatch(/\+23/);
    expect(await screen.findByTestId('trend-value')).toHaveAccessibleName(
      'Trend: +23 (--) compared to previous value of 0',
    );
  });

  it('shows adequate results if previous value is NaN', async () => {
    renderTrend({ current: 23, previous: NaN });

    expect(await findTrend()).toEqual('-- / --');
  });

  it('shows adequate results if current value is 0', async () => {
    renderTrend({ current: 0, previous: 42 });

    expect(await findTrend()).toMatch(/-42 \/ -100\.0%/);
    expect(await screen.findByTestId('trend-value')).toHaveAccessibleName(
      'Trend: -42 (-100.0%) compared to previous value of 42',
    );
  });

  it('shows adequate results if current value is NaN', async () => {
    renderTrend({ current: NaN, previous: 42 });

    expect(await findTrend()).toEqual('-- / --');
  });

  describe('renders icon indicating trend direction', () => {
    it('shows circle right if values are equal', async () => {
      renderTrend();

      const trendIcon = await screen.findByTestId('trend-icon');

      within(trendIcon).getByText('arrow_circle_right');
    });

    it('shows circle down if current values is lower', async () => {
      renderTrend({ current: 41 });
      const trendIcon = await screen.findByTestId('trend-icon');

      within(trendIcon).getByText('arrow_circle_down');
      expect(await screen.findByTestId('trend-value')).toHaveAccessibleName(
        'Trend: -1 (-2.4%) compared to previous value of 42',
      );
    });

    it('shows circle up if current values is higher', async () => {
      renderTrend({ current: 43 });
      const trendIcon = await screen.findByTestId('trend-icon');

      within(trendIcon).getByText('arrow_circle_up');
      expect(await screen.findByTestId('trend-value')).toHaveAccessibleName(
        'Trend: +1 (+2.4%) compared to previous value of 42',
      );
    });
  });
});
