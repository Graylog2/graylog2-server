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
// @flow strict
import * as React from 'react';
import styled, { StyledComponent, css } from 'styled-components';

import Spinner from 'components/common/Spinner';
import { ThemeInterface } from 'theme';
import { Row, Col } from 'components/graylog';

type Props = {
  children: React.ReactNode,
  title: string,
  showLoading?: boolean,
  headerActions?: React.ReactElement,
};

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  *:not(:first-child) {
    margin-left: 10px;
  }
`;

export const Headline: StyledComponent<{}, ThemeInterface, HTMLHeadingElement> = styled.h2`
  margin-bottom: 15px;
  display: inline;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => css`
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const SectionComponent = ({ children, title, showLoading = false, headerActions }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Header>
        <Headline>
          {title}
          {showLoading && <LoadingSpinner text="" delay={0} />}
        </Headline>
        {headerActions}
      </Header>
      {children}
    </Col>
  </Row>
);

SectionComponent.defaultProps = {
  showLoading: false,
  headerActions: undefined,
};

export default SectionComponent;
