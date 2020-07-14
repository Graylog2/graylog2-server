// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { Row, Col } from 'components/graylog';

type Props = {
  children: React.Node,
  title: string,
};

const Headline = styled.h2`
  margin-bottom: 15px;
`;

const SectionComponent = ({ children, title }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Headline>{title}</Headline>
      {children}
    </Col>
  </Row>
);

export default SectionComponent;
