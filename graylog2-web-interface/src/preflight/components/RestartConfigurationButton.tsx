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
import { useCallback, useState } from 'react';

import { Button } from 'preflight/components/common';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';

type Props = {
  variant?: string,
  compact?: boolean,
  color?: string
}

const ResumeStartupButton = ({ variant, compact = false, color }: Props) => {
  const [isRestartingConfiguration, setIsRestartingConfiguration] = useState(false);
  const onResumeStartup = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm('Are you sure you want to restart the configuration? All previous changes will be deleted.')) {
      fetch('DELETE', qualifyUrl('/api/startOver'), undefined, false)
        .then(() => {
          setIsRestartingConfiguration(true);
        })
        .catch((error) => {
          UserNotification.error(`Resuming startup failed with error: ${error}`,
            'Could not resume startup');
        })
        .finally(() => {
          setIsRestartingConfiguration(false);
        });
    }
  }, [setIsRestartingConfiguration]);

  return (
    <Button variant={variant}
            size={compact ? 'compact-xs' : 'xs'}
            color={color}
            onClick={onResumeStartup}>
      {isRestartingConfiguration ? 'restarting...' : 'restart'}
    </Button>
  );
};

export default ResumeStartupButton;
