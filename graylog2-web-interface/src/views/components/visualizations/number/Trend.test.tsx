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
import { mount } from 'wrappedEnzyme';

import Trend, { TREND_BAD, TREND_GOOD, TREND_NEUTRAL } from './Trend';

const renderTrend = ({
  current = 42,
  previous = 42,
  trendPreference = 'NEUTRAL',
} = {}) => mount(<Trend current={current} previous={previous} trendPreference={trendPreference} />);

describe('Trend', () => {
  it('shows absolute delta', () => {
    const wrapper = renderTrend({ previous: 23 });

    expect(wrapper).toIncludeText('+19');
  });

  it('shows relative delta as percentage', () => {
    const wrapper = renderTrend({ previous: 23 });

    expect(wrapper).toIncludeText('+82.61%');
  });

  it('shows absolute delta if values are equal', () => {
    const wrapper = renderTrend();

    expect(wrapper).toIncludeText('+0');
  });

  it('shows relative delta as percentage if values are equal', () => {
    const wrapper = renderTrend();

    expect(wrapper).toIncludeText('+0%');
  });

  it('shows negative absolute delta', () => {
    const wrapper = renderTrend({ current: 23 });

    expect(wrapper).toIncludeText('-19');
  });

  it('shows negative relative delta as percentage', () => {
    const wrapper = renderTrend({ current: 23 });

    expect(wrapper).toIncludeText('-45.24%');
  });

  describe('renders background according to values and trend preference', () => {
    it('shows neutral background if values are equal', () => {
      const wrapper = renderTrend();

      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toExist();
      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toHaveProp('trend', TREND_NEUTRAL);
    });

    it('shows good background if current value and preference are higher', () => {
      const wrapper = renderTrend({ current: 43, trendPreference: 'HIGHER' });

      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toExist();
      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toHaveProp('trend', TREND_GOOD);
    });

    it('shows good background if current value and preference are lower', () => {
      const wrapper = renderTrend({ current: 41, trendPreference: 'LOWER' });

      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toExist();
      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toHaveProp('trend', TREND_GOOD);
    });

    it('shows bad background if current value is lower but preference is higher', () => {
      const wrapper = renderTrend({ current: 41, trendPreference: 'HIGHER' });

      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toExist();
      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toHaveProp('trend', TREND_BAD);
    });

    it('shows bad background if current value is higher but preference is lower', () => {
      const wrapper = renderTrend({ current: 43, trendPreference: 'LOWER' });

      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toExist();
      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toHaveProp('trend', TREND_BAD);
    });

    it('shows neutral background if current value is higher but preference is neutral', () => {
      const wrapper = renderTrend({ current: 43, trendPreference: 'NEUTRAL' });

      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toExist();
      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toHaveProp('trend', TREND_NEUTRAL);
    });

    it('shows neutral background if current value is lower but preference is neutral', () => {
      const wrapper = renderTrend({ current: 41, trendPreference: 'NEUTRAL' });

      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toExist();
      expect(wrapper.find("[data-test-id='trend-background']").at(0)).toHaveProp('trend', TREND_NEUTRAL);
    });
  });

  describe('renders icon indicating trend direction', () => {
    it('shows circle right if values are equal', () => {
      const wrapper = renderTrend();
      const trendIcon = wrapper.find('svg');

      expect(trendIcon).toHaveClassName('fa-arrow-circle-right');
    });

    it('shows circle down if current values is lower', () => {
      const wrapper = renderTrend({ current: 41 });
      const trendIcon = wrapper.find('svg');

      expect(trendIcon).toHaveClassName('fa-arrow-circle-down');
    });

    it('shows circle up if current values is higher', () => {
      const wrapper = renderTrend({ current: 43 });
      const trendIcon = wrapper.find('svg');

      expect(trendIcon).toHaveClassName('fa-arrow-circle-up');
    });
  });
});
