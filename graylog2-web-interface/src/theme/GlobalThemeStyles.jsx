import { createGlobalStyle } from 'styled-components';

import { paginationStyles } from 'components/graylog/Pagination.jsx';
import { progressBarStyles } from 'components/graylog/ProgressBar.jsx';
import globalStyles from './styles/globalStyles';

const GlobalThemeStyles = createGlobalStyle`
  ${globalStyles};
  ${paginationStyles};
  ${progressBarStyles};
`;

export default GlobalThemeStyles;
