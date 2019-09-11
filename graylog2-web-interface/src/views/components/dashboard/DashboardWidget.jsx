import React from 'react';
import PropTypes from 'prop-types';

import { BootstrapModalConfirm } from 'components/bootstrap';
import { widgetDefinition } from 'views/logic/Widgets';

import WidgetFrame from '../widgets/WidgetFrame';
import DashboardWidgetHeader from './DashboardWidgetHeader';

export default class DashboardWidget extends React.Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    widget: PropTypes.shape({
      computationTimeRange: PropTypes.object,
      config: PropTypes.object.isRequired,
    }).isRequired,
    data: PropTypes.any.isRequired,
    height: PropTypes.number,
    width: PropTypes.number,
    fields: PropTypes.any.isRequired,
    onSizeChange: PropTypes.func.isRequired,
    onWidgetDelete: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
  };

  static defaultProps = {
    height: 1,
    width: 1,
  };

  static _visualizationForType(type) {
    return widgetDefinition(type).visualizationComponent;
  }

  constructor(props) {
    super(props);
    this.state = {};
  }

  handleDelete = () => {
    this.deleteConfirmation.open();
  };

  handleDeleteConfirm = (widget) => {
    return () => {
      this.props.onWidgetDelete(widget.id);
      this.deleteConfirmation.close();
    };
  };

  render() {
    const { id, widget, data, height, width, fields, onSizeChange, title } = this.props;
    const { config, computationTimeRange } = widget;
    const VisComponent = DashboardWidget._visualizationForType(widget.type);

    return (
      <WidgetFrame widgetId={id} onSizeChange={onSizeChange}>
        <DashboardWidgetHeader title={title} onDelete={this.handleDelete} />
        <BootstrapModalConfirm ref={(c) => { this.deleteConfirmation = c; }}
                               title="Delete Widget?"
                               onConfirm={this.handleDeleteConfirm(widget)}
                               onCancel={() => {}}>
          <p>Are you sure you want to remove the widget <strong>{title}</strong>?</p>
        </BootstrapModalConfirm>
        <VisComponent id={id}
                      title={widget.title}
                      config={config}
                      data={data}
                      fields={fields}
                      height={height}
                      width={width}
                      onChange={() => {}}
                      onFinishEditing={() => {}}
                      computationTimeRange={computationTimeRange} />
      </WidgetFrame>
    );
  }
}
