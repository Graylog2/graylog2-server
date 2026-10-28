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
import { Trans, useTranslation } from 'react-i18next';

import { Link } from 'components/common';
import Routes from 'routing/Routes';
import usePermissions from 'hooks/usePermissions';

const TimeoutLink = ({ children = undefined }: React.PropsWithChildren<{}>) => {
  const { isPermitted } = usePermissions();

  return isPermitted(['clusterconfigentry:read']) ? (
    <Link to={Routes.SYSTEM.CONFIGURATIONS}>{children}</Link>
  ) : (
    <>{children}</>
  );
};

const GlobalSessionTimeoutText = () => {
  const { t } = useTranslation('users');

  return <Trans t={t} i18nKey="user_form.global_timeout_message" components={{ timeoutLink: <TimeoutLink /> }} />;
};

export default GlobalSessionTimeoutText;
