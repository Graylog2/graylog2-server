import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';

const GetPositionForNewWidget = (widget: Widget, queryId: string, view: View) => {
  const widgetDef = widgetDefinition(widget.type);

  const widgetBuilder = view.state.get(queryId).widgetPositions[widget.id]?.toBuilder()
    || WidgetPosition.builder().width(widgetDef.defaultWidth).height(widgetDef.defaultHeight);

  return widgetBuilder.col(1).row(1).build();
};

export default GetPositionForNewWidget;
