import { createGlobalStyle } from 'styled-components';

import globalStyles from './styles/globalStyles';

const GlobalThemeStyles = createGlobalStyle`
  ${globalStyles};
`;

export default GlobalThemeStyles;
