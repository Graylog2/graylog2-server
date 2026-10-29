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
import numeral from 'numeral';
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import TimeHelper from 'util/TimeHelper';

type Props = {
  metric?: any;
  showTotal?: boolean;
  prefix?: string;
  suffix?: string;
  hideOnZero?: boolean;
  hideOnMissing?: boolean;
} & WithTranslation;

class CounterRate extends React.Component<
  Props,
  {
    prevMetric: any;
    prevTs: any;
    nowTs: number;
  }
> {
  static defaultProps = {
    showTotal: false,
    prefix: null,
    hideOnZero: false,
    hideOnMissing: false,
    metric: undefined,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      prevMetric: null,
      prevTs: null,
      nowTs: TimeHelper.nowInSeconds(),
    };
  }

  UNSAFE_componentWillReceiveProps() {
    this.setState({
      prevMetric: this.props.metric,
      prevTs: this.state.nowTs,
      nowTs: TimeHelper.nowInSeconds(),
    });
  }

  _checkPrevMetric = () => this.state.prevMetric && this.state.prevMetric.count !== undefined && this.state.prevTs;

  _placeholder = () => {
    if (this.props.hideOnZero) {
      return null;
    }

    return <span>{this._prefix()}{this.props.t('counter_rate.calculating')}</span>;
  };

  _prefix = () => {
    if (this.props.prefix) {
      return `${this.props.prefix} `;
    }

    return null;
  };

  _suffix = () => {
    const suffix = this.props.suffix === undefined ? this.props.t('counter_rate.per_second') : this.props.suffix;

    if (suffix) {
      return ` ${suffix}`;
    }

    return null;
  };

  render() {
    if (!(this.props.metric && this.props.metric.count !== undefined)) {
      if (this.props.hideOnMissing) {
        return null;
      }

      if (!this._checkPrevMetric()) {
        return this._placeholder();
      }
    }

    const { count } = this.props.metric;

    let rate = null;

    if (this._checkPrevMetric()) {
      const rateNum = (count - this.state.prevMetric.count) / (this.state.nowTs - this.state.prevTs);

      rate = (
        <span key="rate" className="number-format">
          {this._prefix()}
          {numeral(rateNum).format('0,0')}
          {this._suffix()}
        </span>
      );
    } else {
      return this._placeholder();
    }

    if (this.props.hideOnMissing && !rate) {
      return null;
    }

    return (
      <span>
        {rate}
        {this.props.showTotal && (
          <span key="absolute" className="number-format">
            {' '}
            {this.props.t('counter_rate.total', { total: numeral(count).format('0') })}
          </span>
        )}
      </span>
    );
  }
}

export default withTranslation('system')(CounterRate);
