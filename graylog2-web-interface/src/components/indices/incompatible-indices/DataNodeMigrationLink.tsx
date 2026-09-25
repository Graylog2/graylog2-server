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

import Icon from 'components/common/Icon';
import Link from 'components/common/Link';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';

type Props = {
  text: string;
};

const DataNodeMigrationLink = ({ text }: Props) => {
  const migrationRouteAvailable = !AppConfig.isCloud() && AppConfig.isFeatureEnabled('data_node_migration');

  return migrationRouteAvailable ? (
    <Link to={Routes.SYSTEM.CLUSTER.DATANODE_MIGRATION} target="_blank" rel="noopener noreferrer">
      {text} <Icon name="open_in_new" size="xs" />
      <span className="sr-only"> (opens in a new tab)</span>
    </Link>
  ) : (
    text
  );
};

export default DataNodeMigrationLink;
