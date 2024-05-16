import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import type Widget from 'views/logic/widgets/Widget';
import type { WidgetActionType } from 'views/components/widgets/Types';
import ExportWidgetActionDelegate from 'views/components/widgets/ExportWidgetAction/ExportWidgetActionDelegate';

const ExportWidgetAction: WidgetActionType = {
  type: 'export-widget-action',
  position: 'menu',
  isHidden: (w: Widget) => (w.type !== AggregationWidget.type),
  component: ExportWidgetActionDelegate,
};

export default ExportWidgetAction;
