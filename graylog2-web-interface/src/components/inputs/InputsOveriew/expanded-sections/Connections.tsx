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

import { formatCount } from 'components/inputs/helpers/InputThroughputUtils';

const Connections = ({ openConnections, totalConnections }: { openConnections: number; totalConnections: number }) => {
  const { t } = useTranslation('inputs');

  return (
    <span>
      <Trans
        t={t}
        i18nKey="overview.sections.active_connections"
        values={{ open: formatCount(openConnections), total: formatCount(totalConnections) }}
        components={{ active: <span className="active" />, total: <span className="total" /> }}
      />
      <br />
    </span>
  );
};

export default Connections;
