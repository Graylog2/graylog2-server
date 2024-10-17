import React from 'react';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import SidecarStatus from 'components/sidecars/sidecars/SidecarStatus';
import withParams from 'routing/withParams';
import { CollectorsActions } from 'stores/sidecars/CollectorsStore';
import { SidecarsActions } from 'stores/sidecars/SidecarsStore';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';
import withHistory from 'routing/withHistory';

type SidecarStatusPageProps = {
  params: any;
  history: any;
};

class SidecarStatusPage extends React.Component<SidecarStatusPageProps, {
  [key: string]: any;
}> {
  constructor(props) {
    super(props);

    this.state = {
      sidecar: undefined,
    };
  }

  private interval: NodeJS.Timeout;

  componentDidMount() {
    const reloadSidecar = () => this.reloadSidecar(this.props.history);
    reloadSidecar();
    this.reloadCollectors();
    this.interval = setInterval(reloadSidecar, 5000);
  }

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  reloadSidecar = (history) => {
    SidecarsActions.getSidecar(this.props.params.sidecarId).then(
      (sidecar) => this.setState({ sidecar }),
      (error) => {
        if (error.status === 404) {
          history.push(Routes.SYSTEM.SIDECARS.OVERVIEW);
        }
      },
    );
  };

  reloadCollectors = () => {
    CollectorsActions.all().then((response) => this.setState({ collectors: response.collectors }));
  };

  render() {
    const { sidecar } = this.state;
    const { collectors } = this.state;
    const isLoading = !sidecar || !collectors;

    if (isLoading) {
      return <DocumentTitle title="Sidecar status"><Spinner /></DocumentTitle>;
    }

    return (
      <DocumentTitle title={`Sidecar ${sidecar.node_name} status`}>
        <SidecarsPageNavigation />
        <PageHeader title={<span>Sidecar <em>{sidecar.node_name} status</em></span>}
                    documentationLink={{
                      title: 'Sidecars documentation',
                      path: DocsHelper.PAGES.COLLECTOR_STATUS,
                    }}>
          <span>
            A status overview of the Graylog Sidecar.
          </span>
        </PageHeader>

        <SidecarStatus sidecar={sidecar} collectors={collectors} />
      </DocumentTitle>
    );
  }
}

export default withHistory(withParams<SidecarStatusPageProps>(SidecarStatusPage));
