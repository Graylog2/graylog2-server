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
import { List, Map } from 'immutable';

import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

const fieldType1 = new FieldType('string', ['enumerable'], []);
const fieldTypeMapping1 = new FieldTypeMapping('date', fieldType1);

const fieldType2 = new FieldType('string', ['enumerable'], []);
const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType2);

export const simpleFields = (): List<FieldTypeMapping> => List([fieldTypeMapping1, fieldTypeMapping2]);
export const simpleQueryFields = (queryId: string): Map<string, List<FieldTypeMapping>> => Map({ [queryId]: List([fieldTypeMapping2]) });
