// @flow strict
import { get } from 'lodash';

import { DEFAULT_MESSAGE_FIELDS } from 'views/Constants';
import { WidgetActions } from 'views/stores/WidgetStore';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';

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
    .config(new MessagesWidgetConfig.builder()
      .fields([...DEFAULT_MESSAGE_FIELDS, ...(Object.keys(mergedObject))])
      .showMessageRow(true).build())
    .build();
  return WidgetActions.create(widget);
};

ShowDocumentsHandler.isEnabled = ({ context }) => (context.valuePath !== undefined);

export default ShowDocumentsHandler;
