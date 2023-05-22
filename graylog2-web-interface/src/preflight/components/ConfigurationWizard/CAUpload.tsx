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
import styled from 'styled-components';
import { useCallback } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'preflight/util/UserNotification';
import { Icon, Dropzone } from 'preflight/components/common';
import { qualifyUrl } from 'util/URLUtils';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'preflight/hooks/useDataNodesCA';

const CADropzone = styled(Dropzone)`
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const DropzoneInner = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
`;

const submitUpload = (files: Array<File>) => fetch('POST', qualifyUrl('/api/ca/upload'), { files }, false);

const CAUpload = () => {
  const queryClient = useQueryClient();
  const onRejectUpload = useCallback(() => {
    UserNotification.error('CA upload failed');
  }, []);

  const { mutate: onProcessUpload, isLoading } = useMutation(submitUpload, {
    onSuccess: () => {
      UserNotification.success('CA uploaded successfully');
      queryClient.invalidateQueries(DATA_NODES_CA_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`CA upload failed with error: ${error}`);
    },
  });

  return (
    <CADropzone onDrop={onProcessUpload}
                onReject={onRejectUpload}
                data-testid="upload-dropzone"
                loading={isLoading}>
      <DropzoneInner>
        <Dropzone.Accept>
          <Icon name="file" type="solid" size="2x" />
        </Dropzone.Accept>
        <Dropzone.Reject>
          <Icon name="triangle-exclamation" size="2x" />
        </Dropzone.Reject>
        <Dropzone.Idle>
          <Icon name="file" type="regular" size="2x" />
        </Dropzone.Idle>
        <div>
          Drag CA here or click to select file
        </div>
      </DropzoneInner>
    </CADropzone>
  );
};

export default CAUpload;
