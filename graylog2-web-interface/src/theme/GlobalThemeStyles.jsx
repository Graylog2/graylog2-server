import { createGlobalStyle } from 'styled-components';

import { progressBarStyles } from 'components/graylog/ProgressBar.jsx';

const GlobalThemeStyles = createGlobalStyle`
  ${progressBarStyles};
`;

export default GlobalThemeStyles;
