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

import Alert from 'components/bootstrap/Alert';
import isSecureConnection from 'preflight/util/IsSecureConnection';

type Props = {
  renderIfSecure?: React.ReactElement;
};

const UnsecureConnectionAlert = ({ renderIfSecure = undefined }: Props) => {
  const { t } = useTranslation('preflight');
  const connectionIsSecure = isSecureConnection();

  if (connectionIsSecure === 'YES') {
    return renderIfSecure ?? null;
  }

  return (
    <Alert bsStyle="warning">
      {connectionIsSecure === 'NO' && <>{t('unsecure_connection.not_secure')}</>}
      {connectionIsSecure === 'MAYBE' && <>{t('unsecure_connection.maybe_not_secure')}</>}
    </Alert>
  );
};

export default UnsecureConnectionAlert;
