// eslint-disable-next-line no-restricted-imports
import { Row as BootstrapRow } from 'react-bootstrap';
import styled, { css } from 'styled-components';

export const RowContentStyles = css(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.gray[80]};
  margin-bottom: 9px;
  border-radius: 4px;
`);

const Row = styled(BootstrapRow)`
  &.content {
    ${RowContentStyles}
  }
`;

/** @component */
export default Row;
