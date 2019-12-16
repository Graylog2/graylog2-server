import { createGlobalStyle } from 'styled-components';

import { progressBarStyles } from 'components/graylog/ProgressBar.jsx';
import formControlValidationStyles from './styles/formControlValidationStyles';

const GlobalThemeStyles = createGlobalStyle`
  ${formControlValidationStyles};
  ${progressBarStyles};
`;

export default GlobalThemeStyles;
