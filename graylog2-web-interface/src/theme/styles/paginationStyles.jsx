import { darken } from 'polished';
import { css } from 'styled-components';
import teinte from '../teinte';

const paginationStyles = css`
  &.pagination {
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
  }
`;

export default paginationStyles;
