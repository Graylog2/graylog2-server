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
import * as PropTypes from 'prop-types';

// $FlowFixMe: imports from core need to be fixed in flow
import { LinkContainer } from 'components/graylog/router';
import type { PluginMetadata, Requirements } from 'views/logic/views/View';
import { Col, Row, Button } from 'components/graylog';
import URLUtils from 'util/URLUtils';
import fixup from 'views/pages/StyleFixups.css';
import View from 'views/logic/views/View';
import { viewsPath } from 'views/Constants';

type Props = {
  view: View,
  missingRequirements: Requirements,
};

const MissingRequirements = ({ view, missingRequirements }: Props) => (
  <Row className="content">
    <Col md={6} mdOffset={3} className={fixup.bootstrapHeading}>
      <h1>View: <em>{view.title}</em></h1>
      <p>Unfortunately executing this view is not possible. It uses the following capabilities which are not available:</p>

      <ul>
        {/* $FlowFixMe: Object.entries returns value as `mixed` */}
        {Object.entries(missingRequirements).map(([require, plugin]: [string, PluginMetadata]) => (
          <li key={require}>
            <strong>{require}</strong> - included in <a href={plugin.url} target="_blank" rel="noopener noreferrer">{plugin.name}</a>
          </li>
        ))}
      </ul>
    </Col>

    <Col md={1} mdOffset={8}>
      <LinkContainer to={URLUtils.appPrefixed(viewsPath)}>
        <Button type="submit" bsStyle="success">Back</Button>
      </LinkContainer>
    </Col>
  </Row>
);

MissingRequirements.propTypes = {
  view: PropTypes.instanceOf(View).isRequired,
  missingRequirements: PropTypes.objectOf(PropTypes.shape({
    name: PropTypes.string,
    url: PropTypes.string,
  })).isRequired,
};

export default MissingRequirements;
