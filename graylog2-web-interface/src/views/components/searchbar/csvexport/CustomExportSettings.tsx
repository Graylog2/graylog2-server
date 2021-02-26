import * as React from 'react';
import { useMemo } from 'react';

import Widget from 'views/logic/widgets/Widget';
import { widgetDefinition } from 'views/logic/Widgets';

type Props = {
  widget: Widget,
}

const CustomExportSettings = ({ widget }: Props) => {
  const { exportComponent: ExportComponent = () => null } = useMemo(() => (widget?.type && widgetDefinition(widget.type)) ?? {}, [widget]);

  return <ExportComponent widget={widget} />;
};

export default CustomExportSettings;
