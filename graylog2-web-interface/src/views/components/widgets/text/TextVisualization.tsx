import * as React from 'react';

import type TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';
import Preview from 'components/common/MarkdownEditor/Preview';
import type { WidgetComponentProps } from 'views/types';

const TextVisualization = ({ config, height }: WidgetComponentProps<TextWidgetConfig>) => (
  <Preview value={config?.text} height={height} noBorder show />
);
export default TextVisualization;
