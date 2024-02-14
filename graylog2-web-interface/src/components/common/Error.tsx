import * as React from 'react';
import styled from 'styled-components';

import { Center, Icon } from 'components/common';

const Description = styled.div`
  max-width: 700px;
  display: flex;
  flex-direction: column;
`;

const ErrorIcon = styled(Icon)`
  margin-left: 15px;
  margin-right: 15px;
`;

type Props = {
  error: React.ReactNode,
  title: React.ReactNode,
}
const Error = ({ error, title }: Props) => (
  <Center>
    <ErrorIcon name="exclamation-triangle" size="3x" />
    <Description>
      <strong>{title}</strong>
      <span>{error}</span>
    </Description>
  </Center>
);

export default Error;
