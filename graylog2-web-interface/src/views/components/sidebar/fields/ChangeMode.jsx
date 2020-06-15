// @flow strict
import * as React from 'react';

type Props = {
  changeMode: (newMode: string) => void,
  currentMode: string,
  mode: string,
  text: string,
  title: string,
};

const ChangeMode = ({ changeMode, currentMode, mode, text, title }: Props) => {
  const isCurrentShowFieldsBy = currentMode === mode;
  return (
    // eslint-disable-next-line jsx-a11y/anchor-is-valid,jsx-a11y/click-events-have-key-events
    <a onClick={() => changeMode(mode)}
       role="button"
       tabIndex={0}
       title={title}
       style={{ fontWeight: isCurrentShowFieldsBy ? 'bold' : 'normal' }}>
      {text}
    </a>
  );
};

export default ChangeMode;
