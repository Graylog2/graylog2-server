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
import { Title, Space } from '@mantine/core';

import fetch from 'logic/rest/FetchProvider';
import { Button } from 'preflight/components/common';
import URLUtils from 'util/URLUtils';

const provisionCertificates = () => {
  fetch('POST', URLUtils.qualifyUrl('api/preflight/generate'));
};

const CertificateProvisioning = () => (
  <div>
    <Title order={3}>Provision certificates</Title>
    <p>
      Certificate authority has been configured successfully.<br />
      You can now provision certificate for your data nodes.
    </p>
    <Space h="md" />
    <Button onClick={provisionCertificates}>Provision certificates and continue</Button>
  </div>
);

export default CertificateProvisioning;
