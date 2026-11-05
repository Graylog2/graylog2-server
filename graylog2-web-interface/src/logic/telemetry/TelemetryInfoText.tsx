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
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { ExternalLink } from 'components/common';
import { Alert } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

type Props = {
  showProfile?: boolean;
};

const TelemetryInfoText = ({ showProfile = undefined }: Props) => {
  const { t } = useTranslation('common');
  const productName = useProductName();

  return (
    <Alert bsStyle="info">
      {t('telemetry.info', { productName })}
      <br />
      {t('telemetry.no_personal_data')}
      <br />
      <Trans
        t={t}
        i18nKey="telemetry.learn_more"
        components={{
          privacyLink: <ExternalLink href="https://www.graylog.org/privacy-policy/" />,
        }}
      />
      <br />
      {showProfile ? (
        <Trans t={t} i18nKey="telemetry.turn_off_profile" components={{ bold: <b /> }} />
      ) : (
        t('telemetry.turn_off_here')
      )}
    </Alert>
  );
};

export default TelemetryInfoText;
