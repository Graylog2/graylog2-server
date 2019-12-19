// @flow strict
import { DEFAULT_MESSAGE_FIELDS } from 'views/Constants';
import { WidgetActions } from 'views/stores/WidgetStore';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import TitleTypes from 'views/stores/TitleTypes';

import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import type { ValueActionHandler, ValuePath } from './ValueActionHandler';
import View from '../views/View';
import Widget from '../widgets/Widget';
import { createElasticsearchQueryString } from '../queries/Query';
import { TitlesActions } from '../../stores/TitlesStore';

type Contexts = {
  valuePath: ValuePath,
  view: View,
  widget: Widget,
};

type Arguments = {
  contexts: Contexts;
};

const ShowDocumentsHandler: ValueActionHandler = ({ contexts: { valuePath, widget } }: Arguments) => {
  const mergedObject = valuePath.reduce((elem, acc) => ({ ...acc, ...elem }), {});
  const widgetQuery = widget && widget.query ? widget.query.query_string : '';
  const valuePathQuery = Object.entries(mergedObject)
    .map(([k, v]) => `${k}:${escape(String(v))}`)
    .reduce((prev: string, next: string) => addToQuery(prev, next), '');
  const query = addToQuery(widgetQuery, valuePathQuery);
  const newWidget = MessagesWidget.builder()
    .query(createElasticsearchQueryString(query))
    .newId()
    .config(new MessagesWidgetConfig.builder()
      .fields([...DEFAULT_MESSAGE_FIELDS, ...(Object.keys(mergedObject))])
      .showMessageRow(true).build())
    .build();
  return WidgetActions.create(newWidget).then(() => TitlesActions.set(TitleTypes.Widget, newWidget.id, `Messages for ${valuePathQuery}`));
};

ShowDocumentsHandler.isEnabled = ({ contexts: { valuePath, widget } }) => (valuePath !== undefined && widget !== undefined);

export default ShowDocumentsHandler;
