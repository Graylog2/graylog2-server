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
import { useTranslation } from 'react-i18next';

import Button from 'components/bootstrap/Button';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const ResumeConfigurationButton = () => {
  const { t } = useTranslation('preflight');
  const [isRestartingConfiguration, setIsRestartingConfiguration] = useState(false);
  const onResumeStartup = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('restart_configuration.confirm'))) {
      fetch('DELETE', qualifyUrl('/api/startOver'), undefined, false)
        .then(() => {
          setIsRestartingConfiguration(true);
        })
        .catch((error) => {
          UserNotification.error(t('resume_startup.error', { error: String(error) }), t('resume_startup.error_title'));
        })
        .finally(() => {
          setIsRestartingConfiguration(false);
        });
    }
  }, [setIsRestartingConfiguration, t]);

  return (
    <Button bsSize="xs" bsStyle="danger" onClick={onResumeStartup}>
      {isRestartingConfiguration ? t('restart_configuration.restarting') : t('restart_configuration.restart')}
    </Button>
  );
};

export default ResumeConfigurationButton;
