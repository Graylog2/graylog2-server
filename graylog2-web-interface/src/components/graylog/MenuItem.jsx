// import { useCallback } from 'react';
// import PropTypes from 'prop-types';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { MenuItem as BootstrapMenuItem } from 'react-bootstrap';

import teinte from 'theme/teinte';
import { StyledDropdownButton } from './DropdownButton';

// const MenuItem = forwardRef(({ children, ...props }, ref) => {


//   return (
//     <StyledMenuItem ref={ref} {...props}>{children}</StyledMenuItem>
//   );
// });

// MenuItem.propTypes = {
//   children: PropTypes.node,
// };

// MenuItem.defaultProps = {
//   children: undefined,
// };

// export default MenuItem;


const MenuItem = styled(BootstrapMenuItem)`
  ${StyledDropdownButton} {
    > li > & {
      color: ${teinte.primary.tre};

      :hover,
      :focus {
        color: ${teinte.primary.uno};
        background-color: ${teinte.secondary.tre};
      }
    }

    > .active > & {
      :hover,
      :focus {
        color: ${teinte.primary.uno};
      background-color: ${teinte.secondary.tre};
      }
    }

    > .disabled > & {
      :hover,
      :focus {
        color: ${teinte.secondary.due};
      }
    }
  }
`;

export default MenuItem;
