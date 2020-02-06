// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import menuItemStyles from './styles/menuItem';

const SplitButton = styled(BootstrapSplitButton)`
  ${props => buttonStyles(props)};

  ~ .btn.dropdown-toggle {
    ${props => buttonStyles(props)};
    ${menuItemStyles({ sibling: true })};
  }
`;

export default SplitButton;
