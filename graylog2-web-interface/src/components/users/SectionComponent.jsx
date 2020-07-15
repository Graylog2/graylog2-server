// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import Spinner from 'components/common/Spinner';
import { type ThemeInterface } from 'theme';
import { Row, Col } from 'components/graylog';

type Props = {
  children: React.Node,
  title: string,
  showLoading: boolean,
};

export const Headline: StyledComponent<{}, ThemeInterface, HTMLHeadingElement> = styled.h2`
  margin-bottom: 15px;
`;

const SectionComponent = ({ children, title, showLoading }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Headline>{title}</Headline>{showLoading && <Spinner />}
      {children}
    </Col>
  </Row>
);

export default SectionComponent;
