/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { MenuItem } from 'components/graylog';
import connect from 'stores/connect';
import IfSearch from 'views/components/search/IfSearch';
import { widgetDefinition } from 'views/logic/Widgets';
import { WidgetActions } from 'views/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import WidgetModel from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import SearchActions from 'views/actions/SearchActions';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import CopyWidgetToDashboard from 'views/logic/views/CopyWidgetToDashboard';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import CSVExportModal from 'views/components/searchbar/csvexport/CSVExportModal';
import MoveWidgetToTab from 'views/logic/views/MoveWidgetToTab';
import { loadDashboard } from 'views/logic/views/Actions';
import { CurrentViewStateActions, CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { IconButton } from 'components/common';

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
import CopyToDashboard from './CopyToDashboardForm';
import MoveWidgetToTabModal from './MoveWidgetToTabModal';
import WidgetErrorBoundary from './WidgetErrorBoundary';
import ReplaySearchButton from './ReplaySearchButton';

import CustomPropTypes from '../CustomPropTypes';
import IfDashboard from '../dashboard/IfDashboard';
import InteractiveContext from '../contexts/InteractiveContext';
import IfInteractive from '../dashboard/IfInteractive';

const WidgetActionsWBar = styled.div`
  > * {
    margin-right: 5px;

    :last-child {
      margin-right: 0;
    }
  }
`;

type Props = {
  id: string,
  view: ViewStoreState,
  widget: WidgetModel,
  data?: Array<unknown>,
  editing?: boolean,
  errors?: Array<{ description: string }>,
  fields: Immutable.List<FieldTypeMapping>,
  height?: number,
  width?: number,
  title: string,
  position: WidgetPosition,
  onSizeChange: () => void,
  onPositionsChange: () => void,
  focusedWidget: string | null | undefined,
};
type State = {
  editing: boolean,
  loading: boolean;
  oldWidget?: WidgetModel,
  showCopyToDashboard: boolean,
  showCsvExport: boolean,
  showMoveWidgetToTab: boolean,
};

/* eslint-disable camelcase */
export type Result = {
  total: number,
  rows: Rows,
  effective_timerange: AbsoluteTimeRange,
};
/* eslint-enable camelcase */

export type OnVisualizationConfigChange = (VisualizationConfig) => void;

export type WidgetProps = {
  config: AggregationWidgetConfig,
  data: { [key: string]: Result },
  editing?: boolean,
  toggleEdit: () => void,
  fields: FieldTypeMappingsList,
  onVisualizationConfigChange: OnVisualizationConfigChange,
  type: string,
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
    view: CustomPropTypes.CurrentView.isRequired,
    widget: PropTypes.instanceOf(WidgetModel).isRequired,
    data: PropTypes.any,
    editing: PropTypes.bool,
    errors: WidgetErrorsList,
    height: PropTypes.number,
    width: PropTypes.number,
    fields: PropTypes.any.isRequired,
    onSizeChange: PropTypes.func.isRequired,
    onPositionsChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    position: PropTypes.instanceOf(WidgetPosition).isRequired,
    focusedWidget: PropTypes.string,
  };

  static defaultProps = {
    height: 1,
    width: 1,
    data: undefined,
    errors: undefined,
    editing: false,
    focusedWidget: undefined,
  };

  constructor(props) {
    super(props);
    const { editing } = props;

    this.state = {
      editing,
      loading: false,
      showCopyToDashboard: false,
      showCsvExport: false,
      showMoveWidgetToTab: false,
    };

    if (editing) {
      this.state = { ...this.state, oldWidget: props.widget };
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

  _onToggleMoveWidgetToTab = () => {
    this.setState(({ showMoveWidgetToTab }) => ({ showMoveWidgetToTab: !showMoveWidgetToTab }));
  };

  _updateDashboardWithNewSearch = (dashboard: View, dashboardId: string) => ({ search: newSearch }) => {
    const newDashboard = dashboard.toBuilder().search(newSearch).build();

    ViewManagementActions.update(newDashboard).then(() => loadDashboard(dashboardId));
  };

  _onMoveWidgetToTab = (widgetId, queryId, keepCopy) => {
    const { view } = this.props;
    const { view: activeView } = view;

    if (!queryId) {
      return;
    }

    const newDashboard = MoveWidgetToTab(widgetId, queryId, activeView, keepCopy);

    if (newDashboard) {
      SearchActions.create(newDashboard.search).then((searchResponse) => {
        const updatedDashboard = newDashboard.toBuilder().search(searchResponse.search).build();

        ViewActions.update(updatedDashboard).then(() => {
          this._onToggleMoveWidgetToTab();

          ViewActions.selectQuery(queryId).then(() => {
            SearchActions.executeWithCurrentState();
          });
        });
      });
    }
  };

  _onCopyToDashboard = (widgetId: string, dashboardId: string | undefined | null): void => {
    const { view } = this.props;
    const { view: activeView } = view;

    if (!dashboardId) {
      return;
    }

    const addWidgetToDashboard = (dashboard: View) => (searchJson) => {
      const search = Search.fromJSON(searchJson);
      const newDashboard = CopyWidgetToDashboard(widgetId, activeView, dashboard.toBuilder().search(search).build());

      if (newDashboard && newDashboard.search) {
        SearchActions.create(newDashboard.search).then(this._updateDashboardWithNewSearch(newDashboard, dashboardId));
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
          oldWidget: undefined,
        };
      }

      RefreshActions.disable();

      return {
        editing: true,
        oldWidget: widget,
      };
    });
  };

  _onToggleCSVExport = () => {
    const { showCsvExport } = this.state;

    this.setState({
      showCsvExport: !showCsvExport,
    });
  }

  _onCancelEdit = () => {
    const { oldWidget } = this.state;

    if (oldWidget) {
      const { id } = this.props;

      WidgetActions.update(id, oldWidget);
    }

    this._onToggleEdit();
  };

  _onWidgetConfigChange = (widgetId, config) => WidgetActions.updateConfig(widgetId, config);

  _setLoadingState = (loading: boolean) => this.setState({ loading });

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
        <VisComponent config={config}
                      data={data}
                      editing={editing}
                      fields={fields}
                      filter={filter}
                      height={height}
                      onConfigChange={(newWidgetConfig) => this._onWidgetConfigChange(id, newWidgetConfig)}
                      setLoadingState={this._setLoadingState}
                      title={title}
                      toggleEdit={this._onToggleEdit}
                      type={widget.type}
                      width={width}
                      id={id} />
      );
    }

    return <LoadingWidget />;
  };

  // TODO: Clean up different code paths for normal/edit modes
  render() {
    const { id, widget, fields, onSizeChange, title, position, onPositionsChange, view, focusedWidget } = this.props;
    const { editing, loading, showCopyToDashboard, showCsvExport, showMoveWidgetToTab } = this.state;
    const { config, type } = widget;
    const visualization = this.visualize();

    if (editing) {
      const EditComponent = _editComponentForType(widget.type);

      return (
        <WidgetColorContext id={id}>
          <EditWidgetFrame>
            <MeasureDimensions>
              <WidgetHeader title={title}
                            hideDragHandle
                            loading={loading}
                            onRename={(newTitle) => TitlesActions.set('widget', id, newTitle)}
                            editing={editing} />
              <EditComponent config={config}
                             fields={fields}
                             editing={editing}
                             id={id}
                             type={widget.type}
                             onChange={(newWidgetConfig) => this._onWidgetConfigChange(id, newWidgetConfig)}>
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
            {(interactive) => (
              <WidgetHeader title={title}
                            hideDragHandle={!interactive}
                            loading={loading}
                            onRename={(newTitle) => TitlesActions.set('widget', id, newTitle)}
                            editing={editing}>
                <WidgetActionsWBar>
                  <IfInteractive>
                    <IfDashboard>
                      <ReplaySearchButton />
                    </IfDashboard>
                    <IconButton name={focusedWidget ? 'compress-arrows-alt' : 'expand-arrows-alt'}
                                title={focusedWidget ? 'Un-focus widget' : 'Focus this widget'}
                                onClick={() => CurrentViewStateActions.focusWidget(id)} />
                    <WidgetHorizontalStretch widgetId={widget.id}
                                             widgetType={widget.type}
                                             onStretch={onPositionsChange}
                                             position={position} />
                    <WidgetActionDropdown>
                      <MenuItem onSelect={this._onToggleEdit}>Edit</MenuItem>
                      <MenuItem onSelect={() => this._onDuplicate(id)}>Duplicate</MenuItem>
                      {type === MessagesWidget.type && <MenuItem onSelect={() => this._onToggleCSVExport()}>Export to CSV</MenuItem>}
                      <IfSearch>
                        <MenuItem onSelect={this._onToggleCopyToDashboard}>Copy to Dashboard</MenuItem>
                      </IfSearch>
                      <IfDashboard>
                        <MenuItem onSelect={this._onToggleMoveWidgetToTab}>Move to Page</MenuItem>
                      </IfDashboard>
                      <MenuItem divider />
                      <MenuItem onSelect={() => this._onDelete(widget)}>Delete</MenuItem>
                    </WidgetActionDropdown>
                    {showCopyToDashboard && (
                      <CopyToDashboard widgetId={id}
                                       onSubmit={this._onCopyToDashboard}
                                       onCancel={this._onToggleCopyToDashboard} />
                    )}
                    {showCsvExport && <CSVExportModal view={view.view} directExportWidgetId={widget.id} closeModal={this._onToggleCSVExport} />}
                    {showMoveWidgetToTab && (
                      <MoveWidgetToTabModal view={view.view}
                                            widgetId={widget.id}
                                            onCancel={this._onToggleMoveWidgetToTab}
                                            onSubmit={this._onMoveWidgetToTab} />
                    )}
                  </IfInteractive>
                </WidgetActionsWBar>
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

export default connect(Widget,
  {
    view: ViewStore,
    currentView: CurrentViewStateStore,
  }, (props) => ({
    ...props,
    focusedWidget: props.currentView.focusedWidget,
  }));
