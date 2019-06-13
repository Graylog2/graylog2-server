// @flow strict
export type AutoInterval = {|
  type: 'auto',
  scaling?: number,
|};

export type TimeUnitInterval = {|
  type: 'timeunit',
  value: number,
  unit: string,
|};

export type Interval = AutoInterval | TimeUnitInterval;
