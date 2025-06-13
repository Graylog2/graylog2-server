import TextWidget from 'views/logic/widgets/TextWidget';
import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';

const defaultText = `
## This is a new text widget!

Please edit me to set the text you want to display. You can include:
   - tables
   - images
   - links
   - lists 
   
and more.
`;

export const CreateTextWidget = () => TextWidget.builder().newId().config(new TextWidgetConfig(defaultText)).build();
export default () => (dispatch: ViewsDispatch) => dispatch(addWidget(CreateTextWidget()));
