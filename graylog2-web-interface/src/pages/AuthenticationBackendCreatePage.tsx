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
import { useTranslation } from 'react-i18next';

import withParams from 'routing/withParams';
import 'components/authentication/bindings'; // Bind all authentication plugins
import { getAuthServicePlugin } from 'util/AuthenticationService';

type Props = {
  params: {
    name: string;
  };
};

const AuthenticationBackendCreatePage = ({ params: { name } }: Props) => {
  const { t } = useTranslation('authentication_backend_create');
  const authService = getAuthServicePlugin(name);

  if (!authService) {
    return <>{t('no_auth_service_plugin', { name })}</>;
  }

  const { createComponent: BackendCreate } = authService;

  return <BackendCreate />;
};

export default withParams(AuthenticationBackendCreatePage);
