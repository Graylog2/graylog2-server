const DEFAULT_SETTINGS = {
  /*
  fieldId: { // Same ID as supplied to <Input />
    value: '',
    defaultValue: '', // Update StepReview.jsx & relevant step(s) if you need to output
  }
  */

  /* Default Advanced Settings */
  awsCloudWatchBatchSize: {
    defaultValue: '10000',
  },
  awsCloudWatchThrottleEnabled: {
    value: true, // We want to default to true on render, but never compare the default
  },
  awsCloudWatchAddFlowLogPrefix: {
    value: true, // We want to default to true on render, but never compare the default
  },
};

export default DEFAULT_SETTINGS;
