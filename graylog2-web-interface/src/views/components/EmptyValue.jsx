// @flow strict
import * as React from 'react';
import styled from 'styled-components';

const Container: React.ComponentType<{}> = styled.i`
  color: darkgray;
`;

const EmptyValue = () => <Container>&lt;Empty Value&gt;</Container>;

export default EmptyValue;
