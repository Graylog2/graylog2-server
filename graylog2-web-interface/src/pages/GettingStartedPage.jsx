// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';

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
      <Row>
        <GettingStarted clusterId={clusterId}
                        masterOs={operatingSystem}
                        masterVersion={version}
                        gettingStartedUrl={GETTING_STARTED_URL}
                        noDismissButton={Boolean(location.query.menu)}
                        onDismiss={_onDismiss} />
      </Row>
    </DocumentTitle>
  );
};

GettingStartedPage.displayName = 'GettingStartedPage';

GettingStartedPage.propTypes = {
  location: PropTypes.object.isRequired,
};

export default connect(
  withLocation(GettingStartedPage),
  { systemStore: SystemStore },
  (props) => ({
    ...props,
    system: props.systemStore.system,
  }),
);
