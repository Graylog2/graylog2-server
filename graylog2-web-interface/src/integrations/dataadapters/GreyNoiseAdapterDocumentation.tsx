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

const GreyNoiseAdapterDocumentation = () => {
  const { t } = useTranslation('integrations');

  return (
    <div>
      <p style={{ marginBottom: 10 }}>
        <Trans
          t={t}
          i18nKey="grey_noise_adapter_documentation.description"
          components={{
            greynoiseLink: <ExternalLink href="https://developer.greynoise.io/" />,
            quickContextLink: <ExternalLink href="https://developer.greynoise.io/reference/ip-lookup-1#quickcheck-1" />,
          }}
        />
      </p>
    </div>
  );
};

export default GreyNoiseAdapterDocumentation;
