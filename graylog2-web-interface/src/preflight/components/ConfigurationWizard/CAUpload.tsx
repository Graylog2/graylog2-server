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
import { Group, Text } from '@mantine/core';
import { Dropzone } from '@mantine/dropzone';
import styled from 'styled-components';
import { useCallback } from 'react';

import UserNotification from 'util/UserNotification';
import { Icon } from 'preflight/components/common';

const CADropzone = styled(Dropzone)`
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const uploadRejectionMessage = (files) => {
  if (files.length > 1) {
    return 'Only one file allowed';
  }

  return 'There was an error';
};

const CAConfiguration = () => {
  const onRejectUpload = useCallback((files: Array<unknown>) => {
    UserNotification.error('CA upload failed', uploadRejectionMessage(files));
  }, []);

  const onProcessUpload = useCallback(() => {
  }, []);

  return (
    <CADropzone onDrop={onProcessUpload}
                onReject={onRejectUpload}
                maxFiles={1}>
      <Group position="center">
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
          <Text inline size="md">
            Drag CA here or click to select file
          </Text>
        </div>
      </Group>
    </CADropzone>
  );
};

export default CAConfiguration;
