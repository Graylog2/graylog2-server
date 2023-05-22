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
import { useQueryClient, useMutation } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { Button, Title, Space, Alert } from 'preflight/components/common';
import URLUtils from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'preflight/hooks/useDataNodesCA';
import useDataNodes from 'preflight/hooks/useDataNodes';

const onProvisionCertificates = () => fetch(
  'POST',
  URLUtils.qualifyUrl('api/generate'),
  undefined,
  false,
);

const CertificateProvisioning = () => {
  const queryClient = useQueryClient();
  const { data: dataNodes, isInitialLoading } = useDataNodes();

  const { mutate: provisionCertificates, isLoading } = useMutation(onProvisionCertificates, {
    onSuccess: () => {
      UserNotification.success('CA provisioned successfully');
      queryClient.invalidateQueries(DATA_NODES_CA_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`CA provisioning failed with error: ${error}`);
    },
  });

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
      <Button onClick={() => provisionCertificates()} disabled={!dataNodes.length}>
        {isLoading ? 'Provisioning certificate...' : 'Provision certificate and continue'}
      </Button>
    </div>
  );
};

export default CertificateProvisioning;
