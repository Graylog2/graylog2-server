import * as React from 'react';

import useInputsStates from 'hooks/useInputsStates';
import MenuItemDotBadge from 'components/navigation/MenuItemDotBadge';

const InputsDotBadge = ({ text }: { text: string }) => {
  const { data, isLoading } = useInputsStates();

  if (isLoading) {
    return null;
  }

  const hasFailedOrSetupInputs = data?.states.some((inputState) =>
    ['FAILED', 'FAILING', 'SETUP'].includes(inputState.state),
  );

  return (
    <MenuItemDotBadge
      text={text}
      title="There are inputs that require your attention"
      showDot={hasFailedOrSetupInputs || false}
    />
  );
};

export default InputsDotBadge;
