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

import connect from 'stores/connect';
import { widgetDefinition } from 'views/logic/Widgets';
import { WidgetActions } from 'views/stores/WidgetStore';
import { TitlesActions } from 'views/stores/TitlesStore';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import WidgetModel from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';
import EditWidgetFrame from './EditWidgetFrame';
import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';
import { WidgetErrorsList } from './WidgetPropTypes';
import SaveOrCancelButtons from './SaveOrCancelButtons';
import WidgetColorContext from './WidgetColorContext';
import WidgetErrorBoundary from './WidgetErrorBoundary';
import WidgetActionsMenu from './WidgetActionsMenu';

import CustomPropTypes from '../CustomPropTypes';
import InteractiveContext from '../contexts/InteractiveContext';

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
};

type State = {
  loading: boolean;
  oldWidget?: WidgetModel,
};

export type Result = {
  total: number,
  rows: Rows,
  effective_timerange: AbsoluteTimeRange,
};

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
    data: PropTypes.any,
    editing: PropTypes.bool,
    errors: WidgetErrorsList,
    fields: PropTypes.any.isRequired,
    height: PropTypes.number,
    id: PropTypes.string.isRequired,
    onPositionsChange: PropTypes.func.isRequired,
    onSizeChange: PropTypes.func.isRequired,
    position: PropTypes.instanceOf(WidgetPosition).isRequired,
    title: PropTypes.string.isRequired,
    view: CustomPropTypes.CurrentView.isRequired,
    widget: PropTypes.instanceOf(WidgetModel).isRequired,
    width: PropTypes.number,
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
      loading: false,
    };

    if (editing) {
      this.state = { ...this.state, oldWidget: props.widget };
    }
  }

  _onEdit = (setWidgetFocusing) => {
    const { widget } = this.props;

    this.setState(() => {
      RefreshActions.disable();
      setWidgetFocusing({ id: widget.id, editing: true });

      return {
        oldWidget: widget,
      };
    });
  };

  _onToggleEdit = (setWidgetEditing) => {
    const { widget, editing } = this.props;

    this.setState(() => {
      if (editing) {
        setWidgetEditing(undefined);

        return {
          oldWidget: undefined,
        };
      }

      RefreshActions.disable();
      setWidgetEditing(widget.id);

      return {
        oldWidget: widget,
      };
    });
  };

  _onCancelEdit = (setWidgetEditing) => {
    const { oldWidget } = this.state;

    if (oldWidget) {
      const { id } = this.props;

      WidgetActions.update(id, oldWidget);
    }

    this._onToggleEdit(setWidgetEditing);
  };

  _onWidgetConfigChange = (widgetId, config) => WidgetActions.updateConfig(widgetId, config);

  _setLoadingState = (loading: boolean) => this.setState({ loading });

  visualize = (setWidgetEditing) => {
    const { data, errors, title } = this.props;

    if (errors && errors.length > 0) {
      return <ErrorWidget errors={errors} />;
    }

    if (data) {
      const { id, widget, height, width, fields, view: { activeQuery: queryId }, editing } = this.props;
      const { config, filter } = widget;
      const VisComponent = _visualizationForType(widget.type);

      return (
        <VisComponent config={config}
                      data={data}
                      editing={editing}
                      fields={fields}
                      filter={filter}
                      height={height}
                      queryId={queryId}
                      onConfigChange={(newWidgetConfig) => this._onWidgetConfigChange(id, newWidgetConfig)}
                      setLoadingState={this._setLoadingState}
                      title={title}
                      toggleEdit={() => this._onToggleEdit(setWidgetEditing)}
                      type={widget.type}
                      width={width}
                      id={id} />
      );
    }

    return <LoadingWidget />;
  };

  // TODO: Clean up different code paths for normal/edit modes
  render() {
    const { id, widget, fields, onSizeChange, title, position, onPositionsChange, view, editing } = this.props;
    const { loading } = this.state;
    const { config } = widget;
    const { focusedWidget, setWidgetEditing } = this.context;
    const isFocused = focusedWidget?.id === id;
    const visualization = this.visualize(setWidgetEditing);
    const EditComponent = _editComponentForType(widget.type);

    return (
      <WidgetColorContext id={id}>
        <WidgetFrame widgetId={id} onSizeChange={onSizeChange}>
          <InteractiveContext.Consumer>
            {(interactive) => (
              <WidgetHeader title={title}
                            hideDragHandle={!interactive || isFocused}
                            loading={loading}
                            onRename={(newTitle) => TitlesActions.set('widget', id, newTitle)}
                            editing={editing}>
                <WidgetActionsMenu isFocused={isFocused}
                                   toggleEdit={() => this._onToggleEdit(setWidgetEditing)}
                                   title={title}
                                   view={view}
                                   editing={editing}
                                   position={position}
                                   onPositionsChange={onPositionsChange} />
              </WidgetHeader>
            )}
          </InteractiveContext.Consumer>
          {editing && (
            <EditWidgetFrame>
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
              <SaveOrCancelButtons onFinish={() => this._onToggleEdit(setWidgetEditing)} onCancel={() => this._onCancelEdit(setWidgetEditing)} />
            </EditWidgetFrame>
          )}
          {!editing && (
            <WidgetErrorBoundary>
              {visualization}
            </WidgetErrorBoundary>
          )}
        </WidgetFrame>
      </WidgetColorContext>
    );
  }
}

Widget.contextType = WidgetFocusContext;

export default connect(Widget, { view: ViewStore });
