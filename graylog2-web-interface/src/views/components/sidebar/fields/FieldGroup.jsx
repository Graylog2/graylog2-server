// @flow strict
import * as React from 'react';

type Props = {
  group: string,
  onSelect: (newGroup: string) => void,
  selected: boolean,
  text: string,
  title: string,
};

const FieldGroup = ({ onSelect, selected, group, text, title }: Props) => (
  // eslint-disable-next-line jsx-a11y/anchor-is-valid,jsx-a11y/click-events-have-key-events
  <a onClick={() => onSelect(group)}
     role="button"
     style={{ fontWeight: selected ? 'bold' : 'normal' }}
     tabIndex={0}
     title={title}>
    {text}
  </a>
);

export default FieldGroup;
