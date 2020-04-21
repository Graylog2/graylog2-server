import React from 'react';
import styled from 'styled-components';

const Title = styled.h3(({ theme }) => `
  height: 30px;

  a {
    color: ${theme.color.global.textDefault};
  }

  .label {
    font-size: 50%;
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
