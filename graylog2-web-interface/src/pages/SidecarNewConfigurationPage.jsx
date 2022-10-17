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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import ConfigurationForm from 'components/sidecars/configuration-forms/ConfigurationForm';
import ConfigurationHelper from 'components/sidecars/configuration-forms/ConfigurationHelper';
import SidecarsSubareaNavigation from 'components/sidecars/common/SidecarsSubareaNavigation';
import DocsHelper from 'util/DocsHelper';

const SidecarNewConfigurationPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'SidecarNewConfigurationPage',

  _variableRenameHandler(oldname, newname) {
    this.configurationForm.replaceConfigurationVariableName(oldname, newname);
  },

  render() {
    return (
      <DocumentTitle title="New Collector Configuration">
        <SidecarsSubareaNavigation />
        <PageHeader title="New Collector Configuration"
                    documentationLink={{
                      title: 'Sidecar documentation',
                      path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
                    }}>
          <span>
            Some words about collector configurations.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={6}>
            <ConfigurationForm ref={(c) => { this.configurationForm = c; }}
                               action="create" />
          </Col>
          <Col md={6}>
            <ConfigurationHelper onVariableRename={this._variableRenameHandler} />
          </Col>
        </Row>
      </DocumentTitle>
    );
  },
});

export default SidecarNewConfigurationPage;
