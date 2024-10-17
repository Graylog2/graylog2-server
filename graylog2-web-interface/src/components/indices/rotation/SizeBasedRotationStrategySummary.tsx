import React from 'react';

import NumberUtils from 'util/NumberUtils';

type SizeBasedRotationStrategySummaryProps = {
  config: any;
};

const SizeBasedRotationStrategySummary = ({
  config,
}: SizeBasedRotationStrategySummaryProps) => {
  const { max_size: maxSize } = config;

  return (
    <div>
      <dl>
        <dt>Index rotation strategy:</dt>
        <dd>Index Size</dd>
        <dt>Max index size:</dt>
        <dd>{maxSize} bytes ({NumberUtils.formatBytes(maxSize)})</dd>
      </dl>
    </div>
  );
};

export default SizeBasedRotationStrategySummary;
