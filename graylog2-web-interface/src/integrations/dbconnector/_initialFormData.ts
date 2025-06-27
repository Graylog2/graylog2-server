const DEFAULT_SETTINGS = {
  /*
  fieldId: { // Same ID as supplied to <Input />
    value: '',
    defaultValue: '', // Update StepReview.jsx & relevant step(s) if you need to output
  }
  */

  /* Default Advanced Settings */

  throttleEnabled: {
    value: false, // We want to default to true on render, but never compare the default
  },
  subscribeToAllLog: {
    value: true, // We want to default to true on render, but never compare the default
  },

  dbConnectorName: {
    value: '',
  },

  enableThrottling: {
    value: undefined,
  },

  queryStatement: {
    value: ' '
  },

  pollingInterval: {
    value: 5,
  },

  stateField: {
    value: ' '
  },

  stateFieldType: {
    value: ' '
  },

  dbType: {
    value: "MongoDB "
  },
  tableName: {
    value: " "
  },
  mongoCollectionName: {
    value: " "
  },
  overrideSource: {
    value: '',
  },

};

export default DEFAULT_SETTINGS;