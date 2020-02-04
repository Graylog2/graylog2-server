// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import styled from 'styled-components';
import { darken } from 'polished';

import teinte from 'theme/teinte';

const Pagination = styled(BootstrapPagination)`
  > li {
    > a,
    > span {
      color: ${teinte.tertiary.quattro};
      background-color: ${teinte.primary.due};
      border-color: ${teinte.secondary.tre};

      &:hover,
      &:focus {
        color: ${darken(0.15, teinte.tertiary.quattro)};
        background-color: ${teinte.secondary.due};
        border-color: ${teinte.secondary.tre};
      }
    }
  }

  > .active > a,
  > .active > span {
    &,
    &:hover,
    &:focus {
      color: ${teinte.primary.due};
      background-color: ${teinte.tertiary.quattro};
      border-color: ${teinte.tertiary.quattro};
    }
  }

  > .disabled {
    > span,
    > span:hover,
    > span:focus,
    > a,
    > a:hover,
    > a:focus {
      color: ${darken(0.25, teinte.secondary.tre)};
      background-color: ${teinte.primary.due};
      border-color: ${teinte.secondary.tre};
    }
  }
`;

/** @component */
export default Pagination;
