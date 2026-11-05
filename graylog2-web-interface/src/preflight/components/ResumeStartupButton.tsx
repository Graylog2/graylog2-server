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
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import fetch from 'logic/rest/FetchProvider';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { qualifyUrl } from 'util/URLUtils';
import Button from 'components/bootstrap/Button';
import UserNotification from 'util/UserNotification';

type Props = {
  setIsWaitingForStartup: React.Dispatch<React.SetStateAction<boolean>>;
  children?: React.ReactNode;
  compact?: boolean;
};

const ResumeStartupButton = ({ setIsWaitingForStartup, children = null, compact = false }: Props) => {
  const { t } = useTranslation('preflight');
  const productName = useProductName();
  const { data: dataNodes } = useDataNodes();

  const onResumeStartup = useCallback(() => {
    if (
      dataNodes?.length ||
      // eslint-disable-next-line no-alert
      window.confirm(t('resume_startup.confirm', { productName }))
    ) {
      const status = dataNodes?.length ? 'finish' : 'skip';

      fetch('POST', qualifyUrl(`/api/status/${status}-config`), undefined, false)
        .then(() => {
          setIsWaitingForStartup(true);
        })
        .catch((error) => {
          setIsWaitingForStartup(false);

          UserNotification.error(t('resume_startup.error', { error: String(error) }), t('resume_startup.error_title'));
        });
    }
  }, [dataNodes?.length, productName, setIsWaitingForStartup, t]);

  return (
    <Button bsStyle="info" bsSize={compact ? 'xs' : 'small'} onClick={onResumeStartup}>
      {children ?? t('resume_startup.button')}
    </Button>
  );
};

export default ResumeStartupButton;
