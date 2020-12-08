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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import history from 'util/History';
import SidecarStatus from 'components/sidecars/sidecars/SidecarStatus';
import withParams from 'routing/withParams';

const { SidecarsActions } = CombinedProvider.get('Sidecars');
const { CollectorsActions } = CombinedProvider.get('Collectors');

class SidecarStatusPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
  };

  state = {
    sidecar: undefined,
  };

  componentDidMount() {
    this.reloadSidecar();
    this.reloadCollectors();
    this.interval = setInterval(this.reloadSidecar, 5000);
  }

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  reloadSidecar = () => {
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
        <span>
          <PageHeader title={<span>Sidecar <em>{sidecar.node_name} status</em></span>}>
            <span>
              A status overview of the Graylog Sidecar.
            </span>

            <span>
              Read more about sidecars and how to set them up in the
              {' '}<DocumentationLink page={DocsHelper.PAGES.COLLECTOR_STATUS} text="Graylog documentation" />.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
                <Button bsStyle="info" className="active">Overview</Button>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.ADMINISTRATION}>
                <Button bsStyle="info">Administration</Button>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.CONFIGURATION}>
                <Button bsStyle="info">Configuration</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <SidecarStatus sidecar={sidecar} collectors={collectors} />
        </span>
      </DocumentTitle>
    );
  }
}

export default withParams(SidecarStatusPage);
