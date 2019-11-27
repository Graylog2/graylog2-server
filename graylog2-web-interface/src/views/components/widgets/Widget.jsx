// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import { browserHistory } from 'react-router';

import Routes from 'routing/Routes';
import { MenuItem } from 'components/graylog';
import connect from 'stores/connect';
import IfSearch from 'views/components/search/IfSearch';

import { widgetDefinition } from 'views/logic/Widgets';
import { WidgetActions } from 'views/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import WidgetModel from 'views/logic/widgets/Widget';
import WidgetConfig from 'views/logic/widgets/WidgetConfig';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import SearchActions from 'views/actions/SearchActions';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import CopyWidgetToDashboard from 'views/logic/views/CopyWidgetToDashboard';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';

import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';
import WidgetActionDropdown from './WidgetActionDropdown';

import WidgetHorizontalStretch from './WidgetHorizontalStretch';
import MeasureDimensions from './MeasureDimensions';
import EditWidgetFrame from './EditWidgetFrame';
import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';
import { WidgetErrorsList } from './WidgetPropTypes';
import SaveOrCancelButtons from './SaveOrCancelButtons';
import WidgetColorContext from './WidgetColorContext';
import IfInteractive from '../dashboard/IfInteractive';
import InteractiveContext from '../contexts/InteractiveContext';
import CopyToDashboard from './CopyToDashboardForm';
import WidgetErrorBoundary from './WidgetErrorBoundary';

type Props = {
  id: string,
  view: ViewStoreState,
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
  showCopyToDashboard: boolean,
};

const _visualizationForType = (type) => {
  return widgetDefinition(type).visualizationComponent;
};

const _editComponentForType = (type) => {
  return widgetDefinition(type).editComponent;
};

class Widget extends React.Component<Props, State> {
  static propTypes = {
    id: PropTypes.string.isRequired,
    view: PropTypes.object.isRequired,
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

  constructor(props) {
    super(props);
    const { editing } = props;
    this.state = {
      editing,
      showCopyToDashboard: false,
    };
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

  _onToggleCopyToDashboard = () => {
    this.setState(({ showCopyToDashboard }) => ({ showCopyToDashboard: !showCopyToDashboard }));
  };

  _onCopyToDashboard = (widgetId: string, dashboardId: ?string): void => {
    const { view } = this.props;
    const { view: activeView } = view;

    if (!dashboardId) {
      return;
    }

    const updateDashboardWithNewSearch = (dashboard: View) => ({ search: newSearch }) => {
      const newDashboard = dashboard.toBuilder().search(newSearch).build();
      ViewManagementActions.update(newDashboard).then(() => {
        browserHistory.push(Routes.pluginRoute('DASHBOARDS_VIEWID')(dashboardId));
      });
    };

    const addWidgetToDashboard = (dashboard: View) => (searchJson) => {
      const search = Search.fromJSON(searchJson);
      const newDashboard = CopyWidgetToDashboard(widgetId, activeView, dashboard.toBuilder().search(search).build());
      if (newDashboard && newDashboard.search) {
        SearchActions.create(newDashboard.search).then(updateDashboardWithNewSearch(newDashboard));
      }
    };

    ViewManagementActions.get(dashboardId).then((dashboardJson) => {
      const dashboard = View.fromJSON(dashboardJson);
      SearchActions.get(dashboardJson.search_id).then(addWidgetToDashboard(dashboard));
    });
    this._onToggleCopyToDashboard();
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
      const VisComponent = _visualizationForType(widget.type);
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
    const { editing, showCopyToDashboard } = this.state;
    const { config } = widget;
    const visualization = this.visualize();
    if (editing) {
      const EditComponent = _editComponentForType(widget.type);
      return (
        <WidgetColorContext id={id}>
          <EditWidgetFrame>
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
                <WidgetErrorBoundary>

                  {visualization}
                </WidgetErrorBoundary>
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
          <InteractiveContext.Consumer>
            {interactive => (
              <WidgetHeader title={title}
                            hideDragHandle={!interactive}
                            onRename={newTitle => TitlesActions.set('widget', id, newTitle)}
                            editing={editing}>
                <IfInteractive>
                  <WidgetHorizontalStretch widgetId={widget.id}
                                           widgetType={widget.type}
                                           onStretch={onPositionsChange}
                                           position={position} />
                  {' '}
                  <WidgetActionDropdown>
                    <MenuItem onSelect={this._onToggleEdit}>Edit</MenuItem>
                    <MenuItem onSelect={() => this._onDuplicate(id)}>Duplicate</MenuItem>
                    <IfSearch>
                      <MenuItem onSelect={this._onToggleCopyToDashboard}>Copy to Dashboard</MenuItem>
                    </IfSearch>
                    <MenuItem divider />
                    <MenuItem onSelect={() => this._onDelete(widget)}>Delete</MenuItem>
                  </WidgetActionDropdown>
                  {showCopyToDashboard
                    && (
                      <CopyToDashboard widgetId={id}
                                       onSubmit={this._onCopyToDashboard}
                                       onCancel={this._onToggleCopyToDashboard} />
                    )}
                </IfInteractive>
              </WidgetHeader>
            )}
          </InteractiveContext.Consumer>
          <WidgetErrorBoundary>

            {visualization}
          </WidgetErrorBoundary>
        </WidgetFrame>
      </WidgetColorContext>
    );
  }
}

export default connect(Widget, { view: ViewStore });
