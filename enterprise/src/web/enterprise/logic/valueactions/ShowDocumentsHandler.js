// @flow strict
import { get } from 'lodash';

import { DEFAULT_MESSAGE_FIELDS } from 'enterprise/Constants';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { escape, addToQuery } from 'enterprise/logic/queries/QueryHelper';

import FieldType from '../fieldtypes/FieldType';
import type { ActionContexts } from '../ActionContext';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import type { ValueActionHandlerWithContext, ValuePath } from './ValueActionHandler';

const ShowDocumentsHandler: ValueActionHandlerWithContext = (queryId: string, field: string, value: any, type: FieldType, context: ActionContexts) => {
  const valuePath: ValuePath = context.valuePath || [];
  const mergedObject = valuePath.reduce((elem, acc) => ({ ...acc, ...elem }), {});
  const widgetFilter = get(context, 'widget.filter', '');
  const filter = Object.entries(mergedObject)
    .map(([k, v]) => `${k}:${escape(String(v))}`)
    .reduce((prev: string, next: string) => addToQuery(prev, next), widgetFilter);
  const widget = MessagesWidget.builder()
    .filter(filter)
    .newId()
    .config(new MessagesWidgetConfig([...DEFAULT_MESSAGE_FIELDS, ...(Object.keys(mergedObject))], true))
    .build();
  return WidgetActions.create(widget);
};

ShowDocumentsHandler.isEnabled = ({ context }) => (context.valuePath !== undefined);

export default ShowDocumentsHandler;
