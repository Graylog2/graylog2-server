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

import { Space } from 'preflight/components/common';

import type { MigrationStepComponentProps } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';

const ExistingDataMigrationQuestion = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => (
  <>
    <p>Do you want to migrate your existing data?</p>
    <Space h="md" />
    <p>Please remove the <code>elasticsearch_hosts</code> line from your Graylog configuration file (<code>server.conf</code>).</p>
    <p>E.g., <code>elasticsearch_hosts = https://admin:admin@opensearch1:9200,https://admin:admin@opensearch2:9200,https://admin:admin@opensearch3:9200</code></p>
    <Space h="md" />
    <p>Once that is done please restart Graylog to finish the migration.</p>
    <Space h="md" />
    <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
  </>
);

export default ExistingDataMigrationQuestion;
