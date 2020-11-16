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
import PropTypes from 'prop-types';

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader } from 'components/common';
import Routes from 'routing/Routes';
import DocumentationLink from 'components/support/DocumentationLink';
import CollectorsAdministrationContainer from 'components/sidecars/administration/CollectorsAdministrationContainer';
import withLocation from 'routing/withLocation';

const SidecarAdministrationPage = ({ location: { query: { node_id: nodeId } } }) => (
  <DocumentTitle title="Collectors Administration">
    <span>
      <PageHeader title="Collectors Administration">
        <span>
          The Graylog collectors can reliably forward contents of log files or Windows EventLog from your servers.
        </span>

        <span>
          Read more about collectors and how to set them up in the
          {' '}<DocumentationLink page={DocsHelper.PAGES.COLLECTOR} text="Graylog documentation" />.
        </span>

        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
            <Button bsStyle="info">Overview</Button>
          </LinkContainer>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.ADMINISTRATION}>
            <Button bsStyle="info">Administration</Button>
          </LinkContainer>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.CONFIGURATION}>
            <Button bsStyle="info">Configuration</Button>
          </LinkContainer>
        </ButtonToolbar>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <CollectorsAdministrationContainer nodeId={nodeId} />
        </Col>
      </Row>
    </span>
  </DocumentTitle>
);

SidecarAdministrationPage.propTypes = {
  location: PropTypes.object.isRequired,
};

export default withLocation(SidecarAdministrationPage);
