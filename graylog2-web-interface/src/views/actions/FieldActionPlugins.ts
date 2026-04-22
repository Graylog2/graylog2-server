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

import CopyFieldToClipboard from 'views/logic/fieldactions/CopyFieldToClipboard';
import ToggleFavoriteField from 'views/logic/fieldactions/ToggleFavoriteField';

export const CopyToClipboardFieldActionPlugin = {
  type: 'copy-field-to-clipboard',
  title: 'Copy field name to clipboard',
  handler: CopyFieldToClipboard,
  isEnabled: () => true,
  resetFocus: false,
};

export const AddFavoriteFieldActionPlugin = {
  type: 'add-field-to-favorite',
  title: 'Add field to favorites',
  handler: ToggleFavoriteField,
  isEnabled: () => true,
  resetFocus: false,
  isHidden: (props) => ToggleFavoriteField.isHidden(true, props),
};

export const RemoveFavoriteFieldActionPlugin = {
  type: 'remove-field-to-favorite',
  title: 'Remove field from favorites',
  handler: ToggleFavoriteField,
  isEnabled: () => true,
  resetFocus: false,
  isHidden: (props) => ToggleFavoriteField.isHidden(false, props),
};
