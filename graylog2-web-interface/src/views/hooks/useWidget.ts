import useWidgets from 'views/hooks/useWidgets';

const useWidget = (widgetId: string) => {
  const widgets = useWidgets();

  return widgets.find((widget) => widget.id === widgetId);
};

export default useWidget;
