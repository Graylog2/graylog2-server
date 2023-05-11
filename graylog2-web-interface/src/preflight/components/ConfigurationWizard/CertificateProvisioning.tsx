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
import { useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import fetch from 'logic/rest/FetchProvider';
import { Button, Title, Space, Alert } from 'preflight/components/common';
import URLUtils from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'preflight/hooks/useDataNodesCA';
import useDataNodes from 'preflight/hooks/useDataNodes';

const provisionCertificates = (queryClient: QueryClient, setIsProvisioning: React.Dispatch<React.SetStateAction<boolean>>) => {
  fetch('POST', URLUtils.qualifyUrl('api/generate'), undefined, false).then(() => {
    UserNotification.success('CA provisioned successfully');
    queryClient.invalidateQueries(DATA_NODES_CA_QUERY_KEY);
  }).catch((error) => {
    UserNotification.error(`CA provisioning failed with error: ${error}`);
  }).finally(() => {
    setIsProvisioning(false);
  });
};

const CertificateProvisioning = () => {
  const queryClient = useQueryClient();
  const { data: dataNodes, isInitialLoading } = useDataNodes();
  const [isProvisioning, setIsProvisioning] = useState(false);

  return (
    <div>
      <Title order={3}>Provision certificates</Title>
      <p>
        Certificate authority has been configured successfully.<br />
        You can now provision certificate for your data nodes.
      </p>
      <Space h="md" />
      {(!dataNodes.length && !isInitialLoading) && (
        <Alert type="warning">
          At least one Graylog data node needs to run before the certificate can be provisioned.
        </Alert>
      )}
      <Button onClick={() => provisionCertificates(queryClient, setIsProvisioning)} disabled={!dataNodes.length}>
        {isProvisioning ? 'Provisioning certificate...' : 'Provision certificate and continue'}
      </Button>
    </div>
  );
};

export default CertificateProvisioning;
