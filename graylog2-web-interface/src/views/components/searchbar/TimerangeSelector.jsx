import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const TimerangeWrap = styled.div(({ theme }) => `
  .form-group {
    margin-bottom: 0;
  }

  .relative {
    color: ${theme.colors.gray[40]};
  }

  .absolute {
    font-size: 12px;
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
