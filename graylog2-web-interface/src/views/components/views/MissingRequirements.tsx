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
import capitalize from 'lodash/capitalize';

import type { PluginMetadata, Requirements } from 'views/logic/views/View';
import { Col, Row, Button } from 'components/bootstrap';
import fixup from 'views/pages/StyleFixups.css';
import type View from 'views/logic/views/View';
import useHistory from 'routing/useHistory';

type Props = {
  view: View,
  missingRequirements: Requirements,
};

const MissingRequirements = ({ view, missingRequirements }: Props) => {
  const history = useHistory();

  return (
    <Row className="content">
      <Col md={6} mdOffset={3} className={fixup.bootstrapHeading}>
        <h1>{capitalize(view.type)}: <em>{view.title}</em></h1>
        <p>Unfortunately executing this {view.type?.toLowerCase()} is not possible. It uses the following capabilities
          which are not available:
        </p>

        <ul>
          {Object.entries(missingRequirements).map(([require, plugin]: [string, PluginMetadata]) => (
            <li key={require}>
              <strong>{require}</strong> - included in
              <a href={plugin.url}
                 target="_blank"
                 rel="noopener noreferrer">{plugin.name}
              </a>
            </li>
          ))}
        </ul>
      </Col>

      <Col md={1} mdOffset={8}>
        <Button bsStyle="success" onClick={() => history.goBack()}>Back</Button>
      </Col>
    </Row>
  );
};

export default MissingRequirements;
