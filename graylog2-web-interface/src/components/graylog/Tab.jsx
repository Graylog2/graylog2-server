// eslint-disable-next-line no-restricted-imports
import { Tab as BootstrapTab } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { util } from 'theme';

const Tab = styled(BootstrapTab)(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${util.colorLevel(theme.colors.variant.info, -5)};
  border-top: 0;
  border-radius: 0 0 4px 4px;
  padding: 9px;
`);

/** @component */
export default Tab;
