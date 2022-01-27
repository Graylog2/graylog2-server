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

import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { Col, Row } from 'components/bootstrap';

const BiggerFontSize = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
`);

const GraylogSecurityHeader = styled.h2`
  margin-bottom: 10px;
`;

/*
  TODO: This is a placeholder promotional page. We are still waiting on copy from marketing
*/
const SecurityPage = () => {
  return (
    <DocumentTitle title="Try Graylog Security">
      <div>
        <PageHeader title="Try Graylog for Security">
          <span>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Asperiores at autem dignissimos, doloremque eaque earum eligendi expedita minus molestias nam numquam officia optio provident quaerat qui sint sit ullam veritatis.</span>
        </PageHeader>

        <HideOnCloud>
          <IfPermitted permissions="freelicenses:create">
            <Row className="content">
              <Col md={6}>
                <GraylogSecurityHeader>Graylog for Security</GraylogSecurityHeader>
                <BiggerFontSize>
                  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Accusantium beatae consequatur ducimus ea eos eveniet, laboriosam molestias nisi, pariatur perferendis porro quae quas quo suscipit veritatis vitae voluptatem voluptates voluptatum!
                </BiggerFontSize>
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
