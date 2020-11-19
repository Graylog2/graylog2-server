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
