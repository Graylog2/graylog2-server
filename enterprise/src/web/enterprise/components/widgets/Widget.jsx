import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import UserNotification from 'util/UserNotification';
import { widgetDefinition } from 'enterprise/logic/Widget';
import CurrentWidgetsActions from 'enterprise/actions/CurrentWidgetsActions';
import CurrentWidgetsStore from 'enterprise/stores/CurrentWidgetsStore';
import CurrentTitlesActions from 'enterprise/actions/CurrentTitlesActions';
import CurrentTitlesStore from 'enterprise/stores/CurrentTitlesStore';
import WidgetFilterActions from 'enterprise/actions/WidgetFilterActions';
import WidgetFilterStore from 'enterprise/stores/WidgetFilterStore';

import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';
import WidgetFilterMenu from './WidgetFilterMenu';
import WidgetActionDropdown from './WidgetActionDropdown';

import styles from './Widget.css';

class Widget extends React.Component {
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
    this.state = {
      editing: false,
    };
  }

  _onDelete = (widget) => {
    if (window.confirm(`Are you sure you want to remove the widget "${this.props.title}"?`)) {
      CurrentWidgetsActions.remove(widget.id);
    }
  };

  _onDuplicate = (widgetId) => {
    CurrentWidgetsActions.duplicate(widgetId);
  };

  _onToggleEdit = () => {
    this.setState(state => ({ editing: !state.editing }));
  };

  _onAddToDashboard = (widgetId) => {
    CurrentWidgetsActions.addToDashboard(widgetId)
      .then(
        () => UserNotification.success('Added widget to dashboard.', 'Success!'),
        e => UserNotification.error(`Failed adding widget to dashboard: ${e}`, 'Error!'),
      );
  };

  _onWidgetConfigChange = (widgetId, config) => {
    CurrentWidgetsActions.updateConfig(widgetId, config);
  };

  render() {
    const { id, widget, data, height, width, fields, onSizeChange, title } = this.props;
    const { config, computationTimeRange } = widget;
    const VisComponent = Widget._visualizationForType(widget.type);
    const { editing } = this.state;
    const filter = this.props.widgetFilters.get(id, '');
    return (
      <WidgetFrame widgetId={id} onSizeChange={onSizeChange}>
        <WidgetHeader title={title}
                      onRename={newTitle => CurrentTitlesActions.set('widget', id, newTitle)}
                      editing={editing}>
          <WidgetFilterMenu onChange={newFilter => WidgetFilterActions.change(id, newFilter)} value={filter}>
            <i className={`fa fa-filter ${styles.widgetActionDropdownCaret} ${filter ? styles.filterSet : styles.filterNotSet}`} />
          </WidgetFilterMenu>
          {' '}
          <WidgetActionDropdown editing={editing}
                                onAddToDashboard={() => this._onAddToDashboard(id)}
                                onDelete={() => this._onDelete(widget)}
                                onDuplicate={() => this._onDuplicate(id)}
                                onToggleEdit={this._onToggleEdit}>
            <i className={`fa fa-chevron-down ${styles.widgetActionDropdownCaret} ${styles.tonedDown}`} />
          </WidgetActionDropdown>
        </WidgetHeader>

        <VisComponent id={id}
                      editing={editing}
                      title={widget.title}
                      config={config}
                      data={data}
                      fields={fields}
                      height={height}
                      width={width}
                      filter={filter}
                      onChange={newWidgetConfig => this._onWidgetConfigChange(id, newWidgetConfig)}
                      onFinishEditing={this._onToggleEdit}
                      computationTimeRange={computationTimeRange} />
      </WidgetFrame>
    );
  }
};

export default connect(Widget, { widgetFilters: WidgetFilterStore });
