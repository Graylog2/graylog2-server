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

import { Title, Space } from 'preflight/components/common';
import ResumeStartupButton from 'preflight/components/ResumeStartupButton';

type Props = {
  setIsWaitingForStartup: React.Dispatch<React.SetStateAction<boolean>>,
}

const ConfigurationFinished = ({ setIsWaitingForStartup }: Props) => (
  <div>
    <Title order={3}>All data nodes are secured and reachable.</Title>
    <p>The provisioning has been successful and all data nodes are secured and reachable.</p>
    <Space h="md" />
    <ResumeStartupButton setIsWaitingForStartup={setIsWaitingForStartup} />
  </div>
);

export default ConfigurationFinished;
