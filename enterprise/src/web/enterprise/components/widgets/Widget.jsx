import React from 'react';
import PropTypes from 'prop-types';

import { widgetDefinition } from 'enterprise/logic/Widget';
import CurrentWidgetsActions from 'enterprise/actions/CurrentWidgetsActions';
import CurrentWidgetsStore from 'enterprise/stores/CurrentWidgetsStore';
import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';

export default class Widget extends React.Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    widget: PropTypes.shape({
      computationTimeRange: PropTypes.object.isRequired,
      config: PropTypes.object.isRequired,
    }).isRequired,
    data: PropTypes.any.isRequired,
    height: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
    fields: PropTypes.any.isRequired,
    onSizeChange: PropTypes.func.isRequired,
  };

  static _visualizationForType(type) {
    return widgetDefinition(type).visualizationComponent;
  }

  constructor(props) {
    super(props);
    this.state = {
      editing: false,
    };
  }

  _onDelete = (widget) => {
    if (window.confirm(`Are you sure you want to remove the widget "${widget.title}"?`)) {
      CurrentWidgetsActions.remove(widget.id);
    }
  };

  _onToggleEdit = () => {
    this.setState(state => ({ editing: !state.editing }));
  };

  _onWidgetConfigChange = (widgetId, config) => {
    CurrentWidgetsActions.updateConfig(widgetId, config);
  };

  render() {
    const { id, widget, data, height, width, fields } = this.props;
    const { onSizeChange } = this.props;
    const { config, computationTimeRange } = widget;
    const VisComponent = Widget._visualizationForType(widget.type);
    const { editing } = this.state;
    return (
      <WidgetFrame widgetId={id} onSizeChange={onSizeChange}>
        <WidgetHeader title={widget.title}
                      onToggleEdit={this._onToggleEdit}
                      onDelete={() => this._onDelete(widget)}
                      editing={editing} />
        <VisComponent id={id}
                      editing={editing}
                      title={widget.title}
                      config={config}
                      data={data}
                      fields={fields}
                      height={height}
                      width={width}
                      onChange={newWidgetConfig => this._onWidgetConfigChange(id, newWidgetConfig)}
                      onFinishEditing={this._onToggleEdit}
                      computationTimeRange={computationTimeRange} />
      </WidgetFrame>
    );
  }
};
