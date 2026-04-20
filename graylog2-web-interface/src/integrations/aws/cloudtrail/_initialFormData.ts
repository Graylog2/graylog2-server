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
type FormFieldSetting = {
  value?: string | number | boolean;
  defaultValue?: string;
};

type FormSettings = {
  [fieldId: string]: FormFieldSetting;
};

const DEFAULT_SETTINGS: FormSettings = {
  /*
  fieldId: { // Same ID as supplied to <Input />
    value: '',
    defaultValue: '', // Update StepReview.jsx & relevant step(s) if you need to output
  }
  */

  /* Default Advanced Settings */
  awsCloudTrailThrottleEnabled: {
    value: true, // We want to default to true on render, but never compare the default
  },
  overrideSource: {
    value: '',
  },
  pollingInterval: {
    value: 1,
  },
  sqsMessageBatchSize: {
    value: 5,
  },
  includeFullMessageJson: {
    value: false,
  },
};

export default DEFAULT_SETTINGS;
