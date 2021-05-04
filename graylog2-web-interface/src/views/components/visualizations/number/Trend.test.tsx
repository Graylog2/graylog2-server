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
import { render, screen } from 'wrappedTestingLibrary';

import Trend from './Trend';

const renderTrend = ({
  current = 42,
  previous = 42,
  trendPreference = 'NEUTRAL',
}: Partial<React.ComponentProps<typeof Trend>> = {}) => render(<Trend current={current} previous={previous} trendPreference={trendPreference} />);

describe('Trend', () => {
  it('shows absolute delta', async () => {
    renderTrend({ previous: 23 });

    await screen.findByText(/\+19/);
  });

  it('shows relative delta as percentage', async () => {
    renderTrend({ previous: 23 });

    await screen.findByText(/\+82.61%/);
  });

  it('shows absolute delta if values are equal', async () => {
    renderTrend();

    await screen.findByText(/\+0/);
  });

  it('shows relative delta as percentage if values are equal', async () => {
    renderTrend();

    await screen.findByText(/\+0%/);
  });

  it('shows negative absolute delta', async () => {
    renderTrend({ current: 23 });

    await screen.findByText(/-19/);
  });

  it('shows negative relative delta as percentage', async () => {
    renderTrend({ current: 23 });

    await screen.findByText(/-45.24%/);
  });

  describe('renders background according to values and trend preference', () => {
    it('shows neutral background if values are equal', async () => {
      renderTrend();

      const background = await screen.findByTestId('trend-background');

      expect(background).toHaveStyleRule('background-color', '#fff !important');
    });

    it('shows good background if current value and preference are higher', async () => {
      renderTrend({ current: 43, trendPreference: 'HIGHER' });

      const background = await screen.findByTestId('trend-background');

      expect(background).toHaveStyleRule('background-color', '#00ae42 !important');
    });

    it('shows good background if current value and preference are lower', async () => {
      renderTrend({ current: 41, trendPreference: 'LOWER' });

      const background = await screen.findByTestId('trend-background');

      expect(background).toHaveStyleRule('background-color', '#00ae42 !important');
    });

    it('shows bad background if current value is lower but preference is higher', async () => {
      renderTrend({ current: 41, trendPreference: 'HIGHER' });

      const background = await screen.findByTestId('trend-background');

      expect(background).toHaveStyleRule('background-color', '#ad0707 !important');
    });

    it('shows bad background if current value is higher but preference is lower', async () => {
      renderTrend({ current: 43, trendPreference: 'LOWER' });

      const background = await screen.findByTestId('trend-background');

      expect(background).toHaveStyleRule('background-color', '#ad0707 !important');
    });

    it('shows neutral background if current value is higher but preference is neutral', async () => {
      renderTrend({ current: 43, trendPreference: 'NEUTRAL' });

      const background = await screen.findByTestId('trend-background');

      expect(background).toHaveStyleRule('background-color', '#fff !important');
    });

    it('shows neutral background if current value is lower but preference is neutral', async () => {
      renderTrend({ current: 41, trendPreference: 'NEUTRAL' });

      const background = await screen.findByTestId('trend-background');

      expect(background).toHaveStyleRule('background-color', '#fff !important');
    });
  });

  describe('renders icon indicating trend direction', () => {
    it('shows circle right if values are equal', async () => {
      renderTrend();

      const trendIcon = await screen.findByTestId('trend-icon');

      expect(trendIcon).toHaveClass('fa-arrow-circle-right');
    });

    it('shows circle down if current values is lower', async () => {
      renderTrend({ current: 41 });
      const trendIcon = await screen.findByTestId('trend-icon');

      expect(trendIcon).toHaveClass('fa-arrow-circle-down');
    });

    it('shows circle up if current values is higher', async () => {
      renderTrend({ current: 43 });
      const trendIcon = await screen.findByTestId('trend-icon');

      expect(trendIcon).toHaveClass('fa-arrow-circle-up');
    });
  });
});
