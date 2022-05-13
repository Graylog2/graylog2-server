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

import React, { useEffect, useState } from 'react';

import { Spinner } from 'components/common';
import usePluginEntities from 'views/logic/usePluginEntities';
import { EnterpriseActions } from 'stores/enterprise/EnterpriseStore';

import IndexerFailuresComponent from './IndexerFailuresComponent';

const IndexerSystemOverviewComponent = () => {
  const [loadIndexerFailuresComponent, setLoadIndexerFailuresComponent] = useState(<Spinner text="Looking for Index Failures..." />);

  const pluginSystemOverview = usePluginEntities('systemOverview');
  const EnterpriseIndexerFailures = pluginSystemOverview?.[0]?.component;

  useEffect(() => {
    if (EnterpriseIndexerFailures) {
      EnterpriseActions.getLicenseInfo().then((response) => {
        setLoadIndexerFailuresComponent(response.license_info.license_status === 'installed' ? <EnterpriseIndexerFailures /> : <IndexerFailuresComponent />);
      });
    } else {
      setLoadIndexerFailuresComponent(<IndexerFailuresComponent />);
    }
  }, [EnterpriseIndexerFailures, setLoadIndexerFailuresComponent]);

  return loadIndexerFailuresComponent;
};

export default IndexerSystemOverviewComponent;
