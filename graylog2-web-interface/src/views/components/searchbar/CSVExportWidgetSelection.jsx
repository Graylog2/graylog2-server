// @flow strict
import React from 'react';
import { Map } from 'immutable';

import type { ViewStateMap } from 'views/logic/views/View';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import Widget from 'views/logic/widgets/Widget';

import { Row } from 'components/graylog';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import Select from 'views/components/Select';


type WidgetSelectionProps = {
  selectWidget: {label: string, value: Widget} => void,
  widgets: Map<string, Widget>,
  viewStates: ViewStateMap
}

const WidgetSelection = ({ selectWidget, widgets, viewStates }: WidgetSelectionProps) => {
  const widgetTitles = viewStates.map(state => state.titles.get('widget')).flatten(true);
  const widgetOption = widget => ({ label: widgetTitles.get(widget.id) || MessagesWidget.defaultTitle, value: widget });
  const widgetOptions = widgets.map(widget => (widgetOption(widget))).toArray();
  return (
    <>
      <Row>
        <IfSearch>
          Please select a message table to adopt it&apos;s fields and sort.
        </IfSearch>
        <IfDashboard>
          Please select the message table you want to export.
        </IfDashboard>
      </Row>
      <Row>
        <span>Select widget</span>
        <Select placeholder="Select widget"
                onChange={selectWidget}
                options={widgetOptions} />
      </Row>
    </>
  );
};


export default WidgetSelection;
