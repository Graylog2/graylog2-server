import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import classNames from 'classnames';

import { Row } from 'components/graylog';

const StyledRow = styled(Row)(({ theme }) => css`
  padding-bottom: 0;

  p {
    margin-top: 15px;
  }

  .actions-lg {
    float: right;
  }

  .actions-sm {
    padding-bottom: 15px;
  }

  .description-tooltips .fa-stack {
    margin-right: 3px;
  }

  .btn-lg {
    font-size: ${theme.fonts.size.large};
  }
`);

const ContentHeadRow = ({ children, className, ...props }) => {
  return (
    <StyledRow className={classNames('content-head', className)} {...props}>
      {children}
    </StyledRow>
  );
};

ContentHeadRow.propTypes = {
  children: PropTypes.node.isRequired,
  className: PropTypes.string,
};

ContentHeadRow.defaultProps = {
  className: undefined,
};

export default ContentHeadRow;
