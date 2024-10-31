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

import { Title, Space, Button } from 'preflight/components/common';
import ResumeStartupButton from 'preflight/components/ResumeStartupButton';

type Props = {
  isSkippingProvisioning: boolean,
  setIsWaitingForStartup: React.Dispatch<React.SetStateAction<boolean>>,
  setIsSkippingProvisioning: React.Dispatch<React.SetStateAction<boolean>>,
}

const ConfigurationFinished = ({ setIsWaitingForStartup, isSkippingProvisioning, setIsSkippingProvisioning }: Props) => {
  const description = isSkippingProvisioning
    ? (
      <>
        You&apos;ve finished the configuration successfully. You can still
        {' '}<Button variant="light" onClick={() => setIsSkippingProvisioning(false)} size="compact-xs">go back</Button>
        {' '}to provision the certificates.
      </>
    )
    : 'The provisioning has been successful and all data nodes are secured and reachable.';

  return (
    <div>
      <Title order={3}>Configuration finished</Title>
      <p>{description}</p>
      <Space h="md" />
      <ResumeStartupButton setIsWaitingForStartup={setIsWaitingForStartup} />
    </div>
  );
};

export default ConfigurationFinished;
