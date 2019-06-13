// @flow strict
import * as React from 'react';

import type { Interval } from './Interval';
import TimeUnitTimeHistogramPivot from './TimeUnitTimeHistogramPivot';
import AutoTimeHistogramPivot from './AutoTimeHistogramPivot';

type Props = {
  interval: Interval,
  onChange: (Interval) => void,
};

const TypeSpecificTimeHistogramConfiguration = ({ interval, onChange }: Props) => {
  switch (interval.type) {
    case 'auto':
      return <AutoTimeHistogramPivot interval={interval} onChange={onChange} />;
    case 'timeunit':
      return <TimeUnitTimeHistogramPivot interval={interval} onChange={onChange} />;
    default:
      return null;
  }
};

export default TypeSpecificTimeHistogramConfiguration;
