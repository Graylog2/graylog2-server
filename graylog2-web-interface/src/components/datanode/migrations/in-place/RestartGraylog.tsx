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
import React from 'react';

import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import { Space } from 'preflight/components/common';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';

const RestartGraylog = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => (
  <>
    <p>Almost there!</p>
    <p>Please remove the <code>elasticsearch_hosts</code> line from your <code>server.conf</code></p>
    <p>E.g., <code>elasticsearch_hosts = https://admin:admin@opensearch1:9200,https://admin:admin@opensearch2:9200,https://admin:admin@opensearch3:9200</code></p>
    <Space h="md" />
    <MigrationDatanodeList showProvisioningState={false} />
    <p>Please wait for all data nodes to become &apos;AVAILABLE&apos;. Please check the data node&apos;s log if they do
      not become available within 1-2 minutes.
    </p>
    {/* eslint-disable-next-line react/no-unescaped-entities */}
    <p>Once that's done, please restart Graylog to finish the migration.</p>
    <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
  </>
);

export default RestartGraylog;
