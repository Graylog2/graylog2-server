import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

const TimerangeWrap = styled.div(({ theme }) => css`
  flex: 1;
  margin: 0 12px;
  
  .form-group {
    margin-bottom: 0;
  }

  .relative {
    color: ${theme.colors.gray[40]};
  }

  .absolute {
    font-size: ${theme.fonts.size.body};
  }
`);

const TimerangeSelector = ({ className, children, ...restProps }) => {
  return (
    <TimerangeWrap className={className} {...restProps}>
      {children}
    </TimerangeWrap>
  );
};

TimerangeSelector.propTypes = {
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
};

TimerangeSelector.defaultProps = {
  className: undefined,
};

export default TimerangeSelector;
