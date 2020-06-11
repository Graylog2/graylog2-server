// @flow strict
import styled, { type StyledComponent } from 'styled-components';
import { type ThemeInterface } from 'theme';

const SectionSubHeadline: StyledComponent<{}, ThemeInterface, HTMLHeadingElement> = styled.h3`
  margin-bottom: 10px;
`;

export default SectionSubHeadline;
