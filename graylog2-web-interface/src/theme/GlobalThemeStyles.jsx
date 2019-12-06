import { createGlobalStyle } from 'styled-components';

import { progressBarStyles } from 'components/graylog/ProgressBar.jsx';
import { globalStyles } from './styles/GlobalStyles';

const GlobalThemeStyles = createGlobalStyle`
  ${globalStyles};
  ${progressBarStyles};
`;

export default GlobalThemeStyles;
