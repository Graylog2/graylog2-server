// @flow strict
import { List, Map } from 'immutable';

import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

const fieldType1 = new FieldType('string', ['enumerable'], []);
const fieldTypeMapping1 = new FieldTypeMapping('date', fieldType1);

const fieldType2 = new FieldType('string', ['enumerable'], []);
const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType2);

export const simpleFields = (): List<FieldTypeMapping> => List([fieldTypeMapping1, fieldTypeMapping2]);
export const simpleQueryFields = (queryId: string): Map<string, List<FieldTypeMapping>> => Map({ [queryId]: List([fieldTypeMapping2]) });
