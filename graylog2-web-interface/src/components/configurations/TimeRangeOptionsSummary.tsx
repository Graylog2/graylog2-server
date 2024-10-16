import React from 'react';

type Props = { options: { [key: string]: string } };

const TimeRangeOptionsSummary = ({ options }: Props) => {
  let timerangeOptionsSummary = null;

  if (options) {
    timerangeOptionsSummary = Object.keys(options).map((key) => (
      <span key={`timerange-options-summary-${key}`}>
        <dt>{key}</dt>
        <dd>{options[key]}</dd>
      </span>
    ));
  }

  return (
    <dl className="deflist">
      {timerangeOptionsSummary}
    </dl>
  );
};

export default TimeRangeOptionsSummary;
