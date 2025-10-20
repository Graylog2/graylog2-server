import * as React from 'react';
import styled from 'styled-components';

import useInputsStates from 'hooks/useInputsStates';
const Badge = styled.span`
  position: relative;

  &::after {
    display: 'block';
    content: ' ';
    position: absolute;
    width: 8px;
    height: 8px;

    background-color: ${({ theme }) => theme.colors.brand.primary};
    border-radius: 50%;
    top: 0;
    right: -12px;
  }
`;

const InputsDotBadge = ({ text }: { text: string }) => {
  const { data, isLoading } = useInputsStates();

  if (isLoading) {
    return null;
  }

  const hasFailedOrSetupInputs = data?.states.some((inputState) =>
    ['FAILED', 'FAILING', 'SETUP'].includes(inputState.state),
  );

  if (!hasFailedOrSetupInputs) {
    return <span>{text}</span>;
  }

  return <Badge title="Some inputs are in failed state or in setup mode.">{text}</Badge>;
};

export default InputsDotBadge;
