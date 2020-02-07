import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import { Pagination as DeprecatedPagination } from '@react-bootstrap/pagination';
import { css } from 'styled-components';
import { darken } from 'polished';

import teinte from 'theme/teinte';

export const paginationStyles = css`
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

const Pagination = ({
  activePage,
  children,
  first,
  last,
  maxButtons,
  next,
  prev,
  ...props
}) => {
  if (activePage || maxButtons || prev || next || first || last) {
    useEffect(() => {
      /* eslint-disable-next-line no-console */
      console.warn('Graylog Notice: ', 'You have used a deprecated `Pagination` prop, please check the documentation to use the latest `Pagination`.');
    }, []);

    return (
      <DeprecatedPagination activePage={activePage}
                            first={first}
                            last={last}
                            maxButtons={maxButtons}
                            next={next}
                            prev={prev}
                            {...props} />
    );
  }

  return (
    <BootstrapPagination {...props}>{children}</BootstrapPagination>
  );
};

Pagination.propTypes = {
  children: PropTypes.node,

  /** @deprecated No longer used */
  maxButtons: PropTypes.number,
  /** @deprecated Use `active` boolean prop on `<Pagination.Item />` */
  activePage: PropTypes.number,
  /** @deprecated  use `<Pagination.Prev /> instead` */
  prev: PropTypes.bool,
  /** @deprecated use `<Pagination.Next /> instead` */
  next: PropTypes.bool,
  /** @deprecated use `<Pagination.First /> instead` */
  first: PropTypes.bool,
  /** @deprecated use `<Pagination.Last /> instead` */
  last: PropTypes.bool,
};

Pagination.defaultProps = {
  children: null,

  /* NOTE: Deprecated props */
  activePage: null,
  first: false,
  last: false,
  maxButtons: null,
  next: false,
  prev: false,
};

Pagination.First = BootstrapPagination.First;
Pagination.Prev = BootstrapPagination.Prev;
Pagination.Ellipsis = BootstrapPagination.Ellipsis;
Pagination.Item = BootstrapPagination.Item;
Pagination.Next = BootstrapPagination.Next;
Pagination.Last = BootstrapPagination.Last;

export default Pagination;
