import React from 'react';

import { TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY } from 'stores/indices/IndicesStore';

type DeletionRetentionStrategySummaryProps = {
  config: any;
  rotationStrategyClass?: string;
};

const DeletionRetentionStrategySummary = ({
  config,
  rotationStrategyClass
}: DeletionRetentionStrategySummaryProps) => (
  <div>
    <dl>
      <dt>Index retention strategy:</dt>
      <dd>Delete</dd>
      {rotationStrategyClass !== TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY && (
      <>
        <dt>Max number of indices:</dt>
        <dd>{config.max_number_of_indices}</dd>
      </>
      )}
    </dl>
  </div>
);

export default DeletionRetentionStrategySummary;
