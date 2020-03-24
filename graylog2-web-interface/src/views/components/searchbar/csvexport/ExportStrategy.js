// @flow strict
import View from 'views/logic/views/View';
import { Map } from 'immutable';

type ExportStrategy = {
    title: string,
    shouldAllowWidgetSelection: (singleWidgetDownload: boolean, showWidgetSelection: boolean, widgets: Map<string, Widget>) => boolean,
    shouldEnableDownload: (showWidgetSelection: boolean, selectedWidget: ?Widget, selectedFields: { field: string }[]) => boolean,
    shouldShowWidgetSelection: (singleWidgetDownload: boolean, selectedWidget: ?Widget, widgets: Map<string, Widget>) => boolean,
    initialWidget: (widgets: Map<string, Widget>, directExportWidgetId: ?string) => ?Widget,
  }

const _getWidgetById = (widgets, id) => widgets.find(item => item.id === id);

const _initialSearchWidget = (widgets, directExportWidgetId) => {
  if (directExportWidgetId) {
    return _getWidgetById(widgets, directExportWidgetId);
  }
  if (widgets.size === 1) {
    return widgets.first();
  }
  return null;
};

const SearchExportStrategy: ExportStrategy = {
  title: 'Export all search results to CSV',
  shouldEnableDownload: (showWidgetSelection, selectedWidget, selectedFields) => !showWidgetSelection && !!selectedFields && selectedFields.length > 0,
  shouldAllowWidgetSelection: (singleWidgetDownload, showWidgetSelection, widgets) => !singleWidgetDownload && !showWidgetSelection && widgets.size > 1,
  shouldShowWidgetSelection: (singleWidgetDownload, selectedWidget, widgets) => !singleWidgetDownload && !selectedWidget && widgets.size > 1,
  initialWidget: _initialSearchWidget,
};

const DashboardExportStrategy: ExportStrategy = {
  title: 'Export message table search results to CSV',
  shouldEnableDownload: (showWidgetSelection, selectedWidget, selectedFields) => !!selectedWidget && !!selectedFields && selectedFields.length > 0,
  shouldAllowWidgetSelection: (singeWidgetDownload, showWidgetSelection) => !singeWidgetDownload && !showWidgetSelection,
  shouldShowWidgetSelection: (singeWidgetDownload, selectedWidget) => !singeWidgetDownload && !selectedWidget,
  initialWidget: (widget, directExportWidgetId) => (directExportWidgetId ? _getWidgetById(widget, directExportWidgetId) : null),
};

const createExportStrategy = (viewType) => {
  switch (viewType) {
    case View.Type.Dashboard:
      return DashboardExportStrategy;
    case View.Type.Search:
    default:
      return SearchExportStrategy;
  }
};

export default { createExportStrategy };
