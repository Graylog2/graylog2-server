import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import menuItemStyles from './styles/menuItem';
// import { propTypes, defaultProps } from './props/button';

// const SplitButton = forwardRef((props, ref) => {
//   const { active, bsStyle, disabled } = props;
//   const StyledSplitButton = useCallback(styled(BootstrapSplitButton)`
//     ${buttonStyles(props)};

//     ~ .btn.dropdown-toggle {
//       ${buttonStyles(props)};
//     }

//     && { ${menuItemStyles()}; }
//   `, [active, bsStyle, disabled]);

//   return (
//     <StyledSplitButton ref={ref} {...props} />
//   );
// });

// SplitButton.propTypes = propTypes;

// SplitButton.defaultProps = defaultProps;

const SplitButton = React.memo(styled(BootstrapSplitButton)`
  ${props => buttonStyles(props)};

  ~ .btn.dropdown-toggle {
    ${props => buttonStyles(props)};

    ${menuItemStyles};
  }
`);

export default SplitButton;
