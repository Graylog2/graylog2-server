// eslint-disable-next-line no-restricted-imports
import { Tabs as BootstrapTabs } from 'react-bootstrap';
import styled from 'styled-components';

import navTabsStyles from './styles/nav-tabs';

const Tabs = styled(BootstrapTabs)`
  ${navTabsStyles()};
`;

/** @component */
export default Tabs;
