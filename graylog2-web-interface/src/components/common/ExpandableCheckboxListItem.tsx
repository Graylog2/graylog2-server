import * as React from 'react';
import { useEffect, useRef } from 'react';

import { ExpandableListItem } from 'components/common';
import { Checkbox } from 'components/bootstrap';

const Header = ({ checked, readOnly, onChange, children, indeterminate }) => {
  const checkboxRef = useRef<HTMLInputElement>();

  useEffect(() => {
    if (checkboxRef.current) {
      checkboxRef.current.indeterminate = indeterminate;
    }
  }, [indeterminate]);

  return (
    // eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions
    <span onClick={(e) => e.stopPropagation()}>
      <Checkbox
        inputRef={(ref) => {
          checkboxRef.current = ref;
        }}
        title="Select item"
        checked={checked}
        readOnly={readOnly}
        onClick={(e) => e.stopPropagation()}
        onChange={(e) => {
          e.stopPropagation();
          onChange(e);
        }}
        inline>
        {children}
      </Checkbox>
    </span>
  );
};

type Props = {
  checked: boolean;
  children: React.ReactNode;
  header: React.ReactNode;
  indeterminate?: boolean;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  readOnly?: boolean;
  subheader?: React.ReactNode;
  value: string;
};

const ExpandableCheckboxListItem = ({
  checked,
  onChange,
  children,
  readOnly = false,
  header,
  value,
  indeterminate = undefined,
  subheader = undefined,
}: Props) => (
  <ExpandableListItem
    value={value}
    subheader={subheader}
    header={
      <Header onChange={onChange} checked={checked} readOnly={readOnly} indeterminate={indeterminate}>
        {header}
      </Header>
    }>
    {children}
  </ExpandableListItem>
);

export default ExpandableCheckboxListItem;
