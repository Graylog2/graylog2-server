import React from 'react';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';

import PaginationButton from './PaginationButton';

const PagerButtons = ({
  activePage,
  boundaryLinks,
  ellipsis,
  items,
  maxButtons,
  onClick,
}) => {
  const pageButtons = [];

  let startPage;
  let endPage;

  if (maxButtons && maxButtons < items) {
    startPage = Math.max(
      Math.min(
        activePage - Math.floor(maxButtons / 2, 10),
        items - maxButtons + 1,
      ),
      1,
    );

    endPage = startPage + maxButtons - 1;
  } else {
    startPage = 1;
    endPage = items;
  }

  for (let page = startPage; page <= endPage; page += 1) {
    pageButtons.push(
      <PaginationButton key={page}
                        eventKey={page}
                        active={page === activePage}
                        onClick={onClick}>
        {page}
      </PaginationButton>,
    );
  }

  if (ellipsis && boundaryLinks && startPage > 1) {
    if (startPage > 2) {
      pageButtons.unshift(
        <PaginationButton key="ellipsisFirst"
                          disabled>
          <span aria-label="More">
            <Icon name="ellipsis-h" />
          </span>
        </PaginationButton>,
      );
    }

    pageButtons.unshift(
      <PaginationButton key={1}
                        eventKey={1}
                        active={false}
                        onClick={onClick}>
        1
      </PaginationButton>,
    );
  }

  if (ellipsis && endPage < items) {
    if (!boundaryLinks || endPage < items - 1) {
      pageButtons.push(
        <PaginationButton key="ellipsis"
                          disabled>
          <span aria-label="More">
            <Icon name="ellipsis-h" />
          </span>
        </PaginationButton>,
      );
    }

    if (boundaryLinks) {
      pageButtons.push(
        <PaginationButton key={items}
                          eventKey={items}
                          active={false}
                          onClick={onClick}>
          {items}
        </PaginationButton>,
      );
    }
  }

  return pageButtons;
};

PagerButtons.propTypes = {
  activePage: PropTypes.number.isRequired,
  boundaryLinks: PropTypes.bool.isRequired,
  ellipsis: PropTypes.bool.isRequired,
  items: PropTypes.number.isRequired,
  maxButtons: PropTypes.number.isRequired,
  onClick: PropTypes.func.isRequired,
};

export default PagerButtons;
