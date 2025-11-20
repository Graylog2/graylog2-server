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

import type FieldType from 'views/logic/fieldtypes/FieldType';
import type { Message } from 'views/components/messagelist/Types';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';

export type FormattedField = {
  value: any;
  type: FieldType;
  field: string;
  id: string;
};

export type MessageFieldsListProps = {
  fields: Array<FormattedField>;
  isFavorite?: boolean;
  reorderFavoriteFields?: (fields: Array<FormattedField>) => void;
  onFavoriteToggle?: (field: string) => void;
};

export type MessageFieldsComponentProps = {
  message: Message;
  fields: FieldTypeMappingsList;
};
