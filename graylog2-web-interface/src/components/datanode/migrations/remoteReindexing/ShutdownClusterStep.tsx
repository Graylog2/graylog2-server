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
import * as React from 'react';

import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import { Space } from 'preflight/components/common';

const ShutdownClusterStep = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => (
  <>
    <p>The migration from your current <code>OpenSearch</code> to the Data Node is almost done.</p>
    <p>To finish please shut down your <code>OpenSearch</code> cluster before continuing.</p>
    <Space h="md" />
    <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
  </>
);
export default ShutdownClusterStep;
