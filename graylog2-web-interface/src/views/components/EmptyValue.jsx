// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

const Container: StyledComponent<{}, {}, HTMLElement> = styled.i`
  color: darkgray;
`;

const EmptyValue = () => <Container>&lt;Empty Value&gt;</Container>;

export default EmptyValue;
