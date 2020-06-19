import React from 'react';
import styled from 'styled-components';

const Title = styled.h3(({ theme }) => `
  height: 30px;

  a {
    color: ${theme.colors.global.textDefault};
  }

  .label {
    font-size: calc(${theme.fonts.size.small} + 50%);
    line-height: 200%;
    margin-left: 5px;
    vertical-align: bottom;
  }
`);

const MessageDetailsTitle = (props) => {
  return (
    <Title {...props} />
  );
};

export default MessageDetailsTitle;
