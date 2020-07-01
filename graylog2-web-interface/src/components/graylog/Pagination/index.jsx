import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { bsClass, getClassSet, splitBsProps } from 'react-bootstrap/lib/utils/bootstrapUtils';

import { Icon } from 'components/common';

import PagerButtons from './PagerButtons';
import PaginationButton from './PaginationButton';

function Pagination({
  activePage,
  boundaryLinks,
  className,
  ellipsis,
  first,
  items,
  last,
  maxButtons,
  next,
  onSelect,
  prev,
  ...props
}) {
  const [bsProps, elementProps] = splitBsProps(props);

  const classes = getClassSet(bsProps);

  return (
    <ul {...elementProps}
        className={classNames(className, classes)}>
      {first && (
      <PaginationButton eventKey={1}
                        disabled={activePage === 1}
                        onClick={onSelect}>
        <span aria-label="First">
          <Icon name="angle-double-left" />
        </span>
      </PaginationButton>
      )}

      {prev && (
      <PaginationButton eventKey={activePage - 1}
                        disabled={activePage === 1}
                        onClick={onSelect}>
        <span aria-label="Previous">
          <Icon name="angle-left" />
        </span>
      </PaginationButton>
      )}

      <PagerButtons activePage={activePage}
                    items={items}
                    maxButtons={maxButtons}
                    boundaryLinks={boundaryLinks}
                    ellipsis={ellipsis}
                    onClick={onSelect} />

      {next && (
      <PaginationButton eventKey={activePage + 1}
                        disabled={activePage >= items}
                        onClick={onSelect}>
        <span aria-label="Next">
          <Icon name="angle-right" />
        </span>
      </PaginationButton>
      )}

      {last && (
      <PaginationButton eventKey={items}
                        disabled={activePage >= items}
                        onClick={onSelect}>
        <span aria-label="Last">
          <Icon name="angle-double-right" />
        </span>
      </PaginationButton>
      )}
    </ul>
  );
}

Pagination.propTypes = {
  /**
   * Currently active page number, defaults to `1`
   */
  activePage: PropTypes.number,

  /**
   * Total number of pages possible, defaults to `1`
   */
  items: PropTypes.number,

  /**
   * Number of buttons to render, defaults to `10`
   */
  maxButtons: PropTypes.number,

  /**
   * When `false`, will hide the first and the last button page when displaying ellipsis.
   */
  boundaryLinks: PropTypes.bool,

  /**
   * When `false`, will hide the ellipsis icon
   */
  ellipsis: PropTypes.bool,

  /**
   * When `false`, will hide the double left angle icon
   */
  first: PropTypes.bool,

  /**
   * When `false`, will hide the double right angle icon
   */
  last: PropTypes.bool,

  /**
   * When `false`, will hide the left angle icon
   */
  prev: PropTypes.bool,

  /**
   * When `false`, will hide the right angle icon
   */
  next: PropTypes.bool,

  className: PropTypes.string,

  /**
   * Function called when Page is clicked.
   * @returns {nextPageNumber: number, event: function}
   */
  onSelect: PropTypes.func,
};

Pagination.defaultProps = {
  activePage: 1,
  boundaryLinks: true,
  className: undefined,
  ellipsis: true,
  first: true,
  items: 1,
  last: true,
  maxButtons: 10,
  next: true,
  onSelect: () => {},
  prev: true,
};

export default bsClass('pagination', Pagination);
