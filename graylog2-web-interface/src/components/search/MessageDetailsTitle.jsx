import React from 'react';
import styled from 'styled-components';

const Title = styled.h3`
  height: 30px;

  a {
    color: #000;
  }

  .label {
    font-size: 50%;
    line-height: 200%;
    margin-left: 5px;
    vertical-align: bottom;
  }
`;

const MessageDetailsTitle = (props) => {
  return (
    <Title {...props} />
  );
};

export default MessageDetailsTitle;
