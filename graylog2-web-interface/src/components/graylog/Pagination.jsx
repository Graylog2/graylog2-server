// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import styled from 'styled-components';
import { darken } from 'polished';

import { color } from 'theme';

const Pagination = styled(BootstrapPagination)`
  > li {
    > a,
    > span {
      color: ${color.tertiary.quattro};
      background-color: ${color.primary.due};
      border-color: ${color.secondary.tre};

      &:hover,
      &:focus {
        color: ${darken(0.15, color.tertiary.quattro)};
        background-color: ${color.secondary.due};
        border-color: ${color.secondary.tre};
      }
    }
  }

  > .active > a,
  > .active > span {
    &,
    &:hover,
    &:focus {
      color: ${color.primary.due};
      background-color: ${color.tertiary.quattro};
      border-color: ${color.tertiary.quattro};
    }
  }

  > .disabled {
    > span,
    > span:hover,
    > span:focus,
    > a,
    > a:hover,
    > a:focus {
      color: ${darken(0.25, color.secondary.tre)};
      background-color: ${color.primary.due};
      border-color: ${color.secondary.tre};
    }
  }
`;

export default Pagination;
