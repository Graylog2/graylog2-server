/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

import * as React from 'react';
import { useEffect, useRef } from 'react';

import { ExpandableListItem } from 'components/common';
import { Checkbox } from 'components/bootstrap';

/*
 * This component is useful, when you want to render the `ExpandableListItem` with a checkbox in the header.
 * If the children of this component are also selectable but not expandable, use simple checkboxes instead of an implementing another expandable list.
 */
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

type Props = React.PropsWithChildren<{
  checked: boolean;
  expandable?: boolean;
  header: React.ReactNode;
  indeterminate?: boolean;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  readOnly?: boolean;
  subheader?: React.ReactNode;
  value: string;
}>;

const ExpandableCheckboxListItem = ({
  checked,
  children = undefined,
  expandable = undefined,
  header,
  indeterminate = undefined,
  onChange,
  readOnly = false,
  subheader = undefined,
  value,
}: Props) => (
  <ExpandableListItem
    value={value}
    expandable={expandable}
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
