// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import { MenuItem } from 'components/graylog';
// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
import { widgetDefinition } from 'views/logic/Widgets';
import { WidgetActions } from 'views/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import WidgetModel from 'views/logic/widgets/Widget';
import WidgetConfig from 'views/logic/widgets/WidgetConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';
import WidgetFilterMenu from './WidgetFilterMenu';
import WidgetActionDropdown from './WidgetActionDropdown';

import WidgetHorizontalStretch from './WidgetHorizontalStretch';
import MeasureDimensions from './MeasureDimensions';
import styles from './Widget.css';
import EditWidgetFrame from './EditWidgetFrame';
import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';
import { WidgetErrorsList } from './WidgetPropTypes';
import SaveOrCancelButtons from './SaveOrCancelButtons';
import WidgetColorContext from './WidgetColorContext';
import IfDashboard from '../dashboard/IfDashboard';

type Props = {
  id: string,
  widget: WidgetModel,
  data?: Array<*>,
  editing?: boolean,
  errors?: Array<{ description: string }>,
  fields: Immutable.List<FieldTypeMapping>,
  height?: number,
  width?: number,
  title: string,
  position: WidgetPosition,
  onSizeChange: () => void,
  onPositionsChange: () => void,
};
type State = {
  configChanged?: boolean,
  editing: boolean,
  oldConfig?: WidgetConfig,
};

class Widget extends React.Component<Props, State> {
  static propTypes = {
    id: PropTypes.string.isRequired,
    widget: PropTypes.shape({
      id: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired,
      computationTimeRange: PropTypes.object,
      config: PropTypes.object.isRequired,
      filter: PropTypes.string,
    }).isRequired,
    data: PropTypes.any,
    editing: PropTypes.bool,
    errors: WidgetErrorsList,
    height: PropTypes.number,
    width: PropTypes.number,
    fields: PropTypes.any.isRequired,
    onSizeChange: PropTypes.func.isRequired,
    onPositionsChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    position: PropTypes.object.isRequired,
  };

  static defaultProps = {
    height: 1,
    width: 1,
    data: undefined,
    errors: undefined,
    editing: false,
  };

  static _visualizationForType(type) {
    return widgetDefinition(type).visualizationComponent;
  }

  static _editComponentForType(type) {
    return widgetDefinition(type).editComponent;
  }

  constructor(props) {
    super(props);
    const { editing } = props;
    this.state = { editing };
    if (editing) {
      this.state = { ...this.state, oldConfig: props.widget.config };
    }
  }

  _onDelete = (widget) => {
    const { title } = this.props;
    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to remove the widget "${title}"?`)) {
      WidgetActions.remove(widget.id);
    }
  };

  _onDuplicate = (widgetId) => {
    const { title } = this.props;
    WidgetActions.duplicate(widgetId).then((newWidget) => {
      TitlesActions.set(TitleTypes.Widget, newWidget.id, `${title} (copy)`);
    });
  };

  _onToggleEdit = () => {
    const { widget } = this.props;
    this.setState((state) => {
      if (state.editing) {
        return {
          editing: false,
          oldConfig: undefined,
          configChanged: undefined,
        };
      }
      RefreshActions.disable();
      return {
        editing: true,
        oldConfig: widget.config,
      };
    });
  };

  _onCancelEdit = () => {
    const { configChanged } = this.state;
    if (configChanged) {
      const { id } = this.props;
      const { oldConfig } = this.state;
      WidgetActions.updateConfig(id, oldConfig);
    }
    this._onToggleEdit();
  };

  _onWidgetConfigChange = (widgetId, config) => {
    this.setState({ configChanged: true });
    WidgetActions.updateConfig(widgetId, config);
  };

  visualize = () => {
    const { data, errors, title } = this.props;
    if (errors && errors.length > 0) {
      return <ErrorWidget errors={errors} />;
    }
    if (data) {
      const { editing } = this.state;
      const { id, widget, height, width, fields } = this.props;
      const { config, filter } = widget;
      const VisComponent = Widget._visualizationForType(widget.type);
      return (
        <VisComponent id={id}
                      editing={editing}
                      title={title}
                      config={config}
                      data={data}
                      fields={fields}
                      height={height}
                      width={width}
                      filter={filter}
                      toggleEdit={this._onToggleEdit} />
      );
    }
    return <LoadingWidget />;
  };

  // TODO: Clean up different code paths for normal/edit modes
  render() {
    const { id, widget, fields, onSizeChange, title, position, onPositionsChange } = this.props;
    const { editing } = this.state;
    const { config, filter } = widget;
    const visualization = this.visualize();
    if (editing) {
      const EditComponent = Widget._editComponentForType(widget.type);
      return (
        <WidgetColorContext id={id}>
          <EditWidgetFrame widget={widget}>
            <MeasureDimensions>
              <WidgetHeader title={title}
                            hideDragHandle
                            onRename={newTitle => TitlesActions.set('widget', id, newTitle)}
                            editing={editing} />
              <EditComponent config={config}
                             fields={fields}
                             editting={editing}
                             id={id}
                             onChange={newWidgetConfig => this._onWidgetConfigChange(id, newWidgetConfig)}>
                {visualization}
              </EditComponent>
            </MeasureDimensions>
            <SaveOrCancelButtons onFinish={this._onToggleEdit} onCancel={this._onCancelEdit} />
          </EditWidgetFrame>
        </WidgetColorContext>
      );
    }
    return (
      <WidgetColorContext id={id}>
        <WidgetFrame widgetId={id} onSizeChange={onSizeChange}>
          <span>
            <WidgetHeader title={title}
                          onRename={newTitle => TitlesActions.set('widget', id, newTitle)}
                          editing={editing}>
              <WidgetHorizontalStretch widgetId={widget.id}
                                       widgetType={widget.type}
                                       onStretch={onPositionsChange}
                                       position={position} />
              {' '}
              <WidgetActionDropdown>
                <MenuItem onSelect={this._onToggleEdit}>Edit</MenuItem>
                <MenuItem onSelect={() => this._onDuplicate(id)}>Duplicate</MenuItem>
                <MenuItem divider />
                <MenuItem onSelect={() => this._onDelete(widget)}>Delete</MenuItem>
              </WidgetActionDropdown>
            </WidgetHeader>
            {visualization}
          </span>
        </WidgetFrame>
      </WidgetColorContext>
    );
  }
}

export default connect(Widget, { view: ViewMetadataStore });
