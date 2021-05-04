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
import styled from 'styled-components';

import connect from 'stores/connect';
import { DocumentTitle, Spinner } from 'components/common';
import { Row } from 'components/graylog';
import GettingStarted from 'components/gettingstarted/GettingStarted';
import Routes from 'routing/Routes';
import history from 'util/History';
import StoreProvider from 'injection/StoreProvider';
import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';

const SystemStore = StoreProvider.getStore('System');

const GETTING_STARTED_URL = 'https://gettingstarted.graylog.org/';

const StyledRow = styled(Row)`
  height: 100%;
`;

type Props = {
  system: {
    cluster_id: string,
    operating_system: string,
    version: string,
  },
  location: Location,
};

const GettingStartedPage = ({ system, location }: Props) => {
  if (!system) {
    return <Spinner />;
  }

  const { cluster_id: clusterId, operating_system: operatingSystem, version } = system;
  const _onDismiss = () => history.push(Routes.STARTPAGE);

  return (
    <DocumentTitle title="Getting started">
      <StyledRow>
        <GettingStarted clusterId={clusterId}
                        masterOs={operatingSystem}
                        masterVersion={version}
                        gettingStartedUrl={GETTING_STARTED_URL}
                        noDismissButton={Boolean(location.query.menu)}
                        onDismiss={_onDismiss} />
      </StyledRow>
    </DocumentTitle>
  );
};

GettingStartedPage.displayName = 'GettingStartedPage';

export default connect(
  withLocation(GettingStartedPage),
  { systemStore: SystemStore },
  ({ systemStore }: { systemStore: { system: unknown }}) => ({
    system: systemStore.system,
  }),
);
