// @flow strict
import { get } from 'lodash';

import { DEFAULT_MESSAGE_FIELDS } from 'views/Constants';
import { WidgetActions } from 'views/stores/WidgetStore';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';

import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import type { ValueActionHandler, ValuePath } from './ValueActionHandler';

const ShowDocumentsHandler: ValueActionHandler = ({ contexts }) => {
  const valuePath: ValuePath = contexts.valuePath || [];
  const mergedObject = valuePath.reduce((elem, acc) => ({ ...acc, ...elem }), {});
  const widgetFilter = get(contexts, 'widget.filter', '');
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

ShowDocumentsHandler.isEnabled = ({ contexts: { valuePath } }) => (valuePath !== undefined);

export default ShowDocumentsHandler;
