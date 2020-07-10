// @flow strict
// eslint-disable-next-line no-restricted-imports
import { Row as BootstrapRow } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

type RowContentType = {
  theme: ThemeInterface,
};

export const RowContentStyles = css(({ theme }: RowContentType) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.gray[80]};
  margin-bottom: 9px;
  border-radius: 4px;
`);

const Row: StyledComponent<{children: any}, void, *> = styled(BootstrapRow)`
  &.content {
    ${RowContentStyles}
  }
`;

/** @component */
export default Row;
