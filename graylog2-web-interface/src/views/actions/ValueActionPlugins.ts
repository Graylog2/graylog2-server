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

import CopyValueToClipboard from 'views/logic/valueactions/CopyValueToClipboard';
import SelectExtractorType from 'views/logic/valueactions/SelectExtractorType';

export const CopyToClipBoardValueActionPlugin = {
  type: 'copy-value-to-clipboard',
  title: 'Copy value to clipboard',
  handler: CopyValueToClipboard,
  isEnabled: CopyValueToClipboard.isEnabled,
  resetFocus: false,
};

export const CreateExtractorValueActionPlugin = {
  type: 'create-extractor',
  title: 'Create extractor',
  isEnabled: ({ type, contexts }) => !!contexts.message && !type.isDecorated() && !!contexts.isLocalNode,
  component: SelectExtractorType,
  resetFocus: false,
};
