// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { darken } from 'polished';

const Pagination = styled(BootstrapPagination)(({ theme }) => css`
  > li {
    > a,
    > span {
      color: ${theme.color.tertiary.quattro};
      background-color: ${theme.color.primary.due};
      border-color: ${theme.color.secondary.tre};

      &:hover,
      &:focus {
        color: ${darken(0.15, theme.color.tertiary.quattro)};
        background-color: ${theme.color.secondary.due};
        border-color: ${theme.color.secondary.tre};
      }
    }
  }

  > .active > a,
  > .active > span {
    &,
    &:hover,
    &:focus {
      color: ${theme.color.primary.due};
      background-color: ${theme.color.tertiary.quattro};
      border-color: ${theme.color.tertiary.quattro};
    }
  }

  > .disabled {
    > span,
    > span:hover,
    > span:focus,
    > a,
    > a:hover,
    > a:focus {
      color: ${darken(0.25, theme.color.secondary.tre)};
      background-color: ${theme.color.primary.due};
      border-color: ${theme.color.secondary.tre};
    }
  }
`);

export default Pagination;
