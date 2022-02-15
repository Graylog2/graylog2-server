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
import styled, { css } from 'styled-components';

import { DocumentTitle, IfPermitted } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import {Col, Row, Alert} from 'components/bootstrap';
import {Link} from 'components/common/router';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';

const StyledH4 = styled.h4`
  font-weight: bold;
  margin-bottom: 5px;
`;

const H2 = styled.h2(({ theme }) => css`
  padding-bottom: ${theme.spacings.sm};
`);

const isCloud = AppConfig.isCloud();

const SecurityPage = () => {
  return (
    <DocumentTitle title="Try Graylog Security">
      <div>
        <HideOnCloud>
          <IfPermitted permissions="freelicenses:create">
            <Row className="content">
              <Col md={12}>
                <H2>Invalid License for Analyst Tools</H2>
                <Alert bsStyle="danger">
                  <StyledH4>Analyst Tools are disabled</StyledH4>
                  <p>
                    Analyst Tools are disabled because a valid Graylog for Security license was not found.
                  </p>
                  {isCloud
                    ? (<>Contact your Graylog account manager.</>)
                    : (
                      <IfPermitted permissions="licenses:create">
                        <p>
                          See <Link to={Routes.pluginRoute('SYSTEM_LICENSES')}>Licenses page</Link> for details.
                        </p>
                      </IfPermitted>
                    )}
                </Alert>
              </Col>
              <Col md={6}>
                {/* Put License Status here */}
              </Col>
            </Row>
          </IfPermitted>
        </HideOnCloud>
      </div>
    </DocumentTitle>
  );
};

export default SecurityPage;
