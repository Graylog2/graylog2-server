// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import styled from 'styled-components';
import { darken } from 'polished';

import { color } from 'theme';
import { readableColor } from 'theme/utils';

const Pagination = styled(BootstrapPagination)`
  > li {
    > a,
    > span {
      color: ${readableColor(color.gray[100])};
      background-color: ${color.gray[100]};
      border-color: ${color.gray[80]};

      &:hover,
      &:focus {
        color: ${darken(0.15, color.variant.primary)};
        background-color: ${color.gray[90]};
        border-color: ${color.gray[80]};
      }
    }
  }

  > .active > a,
  > .active > span {
    &,
    &:hover,
    &:focus {
      color: ${readableColor(color.variant.primary)};
      background-color: ${color.variant.primary};
      border-color: ${color.variant.primary};
    }
  }

  > .disabled {
    > span,
    > span:hover,
    > span:focus,
    > a,
    > a:hover,
    > a:focus {
      color: ${color.gray[60]};
      background-color: ${color.gray[100]};
      border-color: ${color.gray[80]};
    }
  }
`;

export default Pagination;
