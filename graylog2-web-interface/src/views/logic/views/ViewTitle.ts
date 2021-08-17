import { ViewType } from 'views/logic/views/View';
import ViewTypeLabel from 'views/components/ViewTypeLabel';

export default (title: string, type: ViewType) => title ?? `Unsaved ${ViewTypeLabel({ type, capitalize: true })}`;
