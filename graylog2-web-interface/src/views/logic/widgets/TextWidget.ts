import { Map } from 'immutable';

import Widget, { type WidgetState } from 'views/logic/widgets/Widget';
import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';
import type { TimeRange } from 'views/logic/queries/Query';
import type { QueryString } from 'views/logic/queries/types';
import type { FiltersType } from 'views/types';

export default class TextWidget extends Widget {
  static type = 'text';

  static defaultTitle = 'Untitled Text Widget';

  constructor(
    id: string,
    config: TextWidgetConfig,
    filter: string | undefined | null,
    timerange: TimeRange | undefined | null,
    query: QueryString | undefined | null,
    streams: Array<string>,
    streamCategories: Array<string>,
    filters?: FiltersType,
  ) {
    super(id, TextWidget.type, config, filter, timerange, query, streams, streamCategories, filters);
  }

  static fromJSON(value: WidgetState) {
    const { id, config, filter, timerange, query, streams, stream_categories, filters } = value;

    return new TextWidget(
      id,
      TextWidgetConfig.fromJSON(config),
      filter,
      timerange,
      query,
      streams,
      stream_categories,
      filters,
    );
  }

  equals(other: any) {
    if (other instanceof TextWidget) {
      return this._value.id === other.id && this._value.config.equals(other.config);
    }

    return false;
  }

  equalsForSearch(other: any) {
    return other instanceof TextWidget && this._value.config.equalsForSearch(other.config);
  }

  toBuilder() {
    const { id, config, filter, timerange, query, streams, stream_categories, filters } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Map({ id, config, filter, timerange, query, streams, stream_categories, filters }));
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }
}

class Builder extends Widget.Builder {
  build() {
    const { id, config, filter, timerange, query, streams, stream_categories, filters } = this.value.toObject();

    return new TextWidget(id, config, filter, timerange, query, streams, stream_categories, filters);
  }
}
