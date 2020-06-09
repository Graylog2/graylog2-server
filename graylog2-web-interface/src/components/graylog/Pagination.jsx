import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { Pagination as BootstrapPagination } from 'react-bootstrap';
import { Pagination as DeprecatedPagination } from '@react-bootstrap/pagination';
import { css } from 'styled-components';

import deprecationNotice from 'util/deprecationNotice';

export const paginationStyles = css(({ theme }) => css`
  &.pagination {
    > li {
      > a,
      > span {
        color: ${theme.utils.readableColor(theme.colors.global.contentBackground)};
        background-color: ${theme.colors.global.contentBackground};
        border-color: ${theme.colors.gray[80]};

        &:hover,
        &:focus {
          color: ${theme.colors.variant.dark.primary};
          background-color: ${theme.colors.gray[90]};
          border-color: ${theme.colors.gray[80]};
        }
      }
    }

    > .active > a,
    > .active > span {
      &,
      &:hover,
      &:focus {
        color: ${theme.colors.variant.dark.primary};
        background-color: ${theme.colors.gray[90]};
        border-color: ${theme.colors.gray[80]};
      }
    }

    > .disabled {
      > span,
      > span:hover,
      > span:focus,
      > a,
      > a:hover,
      > a:focus {
        color: ${theme.colors.gray[60]};
        background-color: ${theme.colors.gray[80]};
        border-color: ${theme.colors.gray[80]};
      }
    }
  }
`);

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
      deprecationNotice('You have used a deprecated `Pagination` prop, please check the documentation to use the latest `Pagination`.');
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

/** @component */
export default Pagination;
