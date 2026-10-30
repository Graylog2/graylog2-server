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

import { ExpandedSectionToggleWrapper } from 'components/common/EntityDataTable';

import { DETAILS_SECTION } from '../Constants';
import type { IndexSetEntity } from '../types';

type Props = React.PropsWithChildren<{
  indexSet: IndexSetEntity;
  align?: 'right';
}>;

const DetailsToggle = ({ indexSet, align = undefined, children = undefined }: Props) => (
  <ExpandedSectionToggleWrapper id={indexSet.id} section={DETAILS_SECTION} align={align}>
    {children}
  </ExpandedSectionToggleWrapper>
);

export default DetailsToggle;
