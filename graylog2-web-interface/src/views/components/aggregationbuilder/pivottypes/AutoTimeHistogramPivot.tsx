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
// @flow strict
import * as React from 'react';

import { FormControl, HelpBlock } from 'components/graylog';
import { Icon } from 'components/common';
import FormsUtils from 'util/FormsUtils';

import type { AutoInterval, Interval } from './Interval';
import styles from './AutoTimeHistogramPivot.css';

type OnChange = (Interval) => void;

type Props = {
  interval: AutoInterval,
  onChange: OnChange,
};

const _changeScaling = (event: SyntheticInputEvent<HTMLInputElement>, interval: AutoInterval, onChange: OnChange) => {
  const scaling = 1 / FormsUtils.getValueFromInput(event.target);

  onChange({ ...interval, scaling });
};

const AutoTimeHistogramPivot = ({ interval, onChange }: Props) => (
  <>
    <div className={styles.alignSliderWithLabels}>
      <Icon name="search-minus" size="lg" style={{ paddingRight: '0.5rem' }} />
      <FormControl type="range"
                   style={{ padding: 0, border: 0 }}
                   min={0.5}
                   max={10}
                   step={0.5}
                   value={interval.scaling ? (1 / interval.scaling) : 1.0}
                   onChange={(e) => _changeScaling(e, interval, onChange)} />
      <Icon name="search-plus" size="lg" style={{ paddingLeft: '0.5rem' }} />
    </div>
    <div className="pull-right">Currently: {interval.scaling ? (1 / interval.scaling) : 1.0}x</div>
    <HelpBlock className={styles.helpBlock}>
      A smaller granularity leads to <strong>less</strong>, a bigger to <strong>more</strong> values.
    </HelpBlock>
  </>
);

export default AutoTimeHistogramPivot;
