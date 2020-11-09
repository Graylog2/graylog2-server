// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { Field, useFormikContext } from 'formik';

import connect from 'stores/connect';
import { Col, Row } from 'components/graylog';
import { Icon } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import Button from 'components/graylog/Button';
import TopRow from 'views/components/searchbar/TopRow';
import { StreamsStore } from 'views/stores/StreamsStore';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import SearchActions from 'views/actions/SearchActions';

import TimeRangeTypeSelector from './searchbar/TimeRangeTypeSelector';
import StreamsFilter from './searchbar/StreamsFilter';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/AsyncQueryInput';

type Props = {
  availableStreams: Array<any>,
  config: any,
  globalOverride: ?GlobalOverride,
};

const StyledTimeRange = styled.input`
  width: 100%;
  padding: 3px 9px;
  margin: 0 12px;
`;

const FlexCol = styled(Col)`
  display: flex;
  align-items: stretch;
  justify-content: space-between;
`;

const BlurredWrapper = styled.div`
  filter: blur(4px);
`;

const CenteredBox = styled.div`
  position: absolute;
  background: white;
  padding: 10px 15px 10px 15px;
  border-color: lightgray;
  border-radius: 2px;
  border-width: 1px;
  border-style: solid;
  box-shadow: 3px 3px 3px darkgrey;
  z-index: 1;
  left: 0;
  right: 0;
  width: max-content;
  margin: 0 auto;
`;

const ResetFilterButton = styled(Button)`
  margin-left: 5px;
  vertical-align: initial;
`;

const _resetOverride = () => GlobalOverrideActions.reset().then(SearchActions.refresh);

const ResetOverrideHint = () => (
  <CenteredBox>
    These controls are disabled, because a filter is applied to all widgets.{' '}
    <ResetFilterButton bsSize="xs" bsStyle="primary" data-testid="reset-filter" onClick={_resetOverride}>Reset filter</ResetFilterButton>
  </CenteredBox>
);

const WidgetQueryControls = ({ availableStreams, config, globalOverride = {} }: Props) => {
  const isGloballyOverridden: boolean = globalOverride !== undefined
    && globalOverride !== null
    && (globalOverride.query !== undefined || globalOverride.timerange !== undefined);
  const Wrapper = isGloballyOverridden ? BlurredWrapper : React.Fragment;
  const { dirty, isValid, isSubmitting, handleSubmit, values } = useFormikContext();

  return (
    <>
      {isGloballyOverridden && <ResetOverrideHint />}
      <Wrapper>
        <>
          <TopRow>
            <FlexCol md={4}>
              <TimeRangeTypeSelector disabled={isGloballyOverridden}
                                     config={config} />
              <StyledTimeRange type="text"
                               value={JSON.stringify(values?.timerange)}
                               disabled />
            </FlexCol>

            <Col md={8}>
              <Field name="streams">
                {({ field: { name, value, onChange } }) => (
                  <StreamsFilter value={value}
                                 disabled={isGloballyOverridden}
                                 streams={availableStreams}
                                 onChange={(newStreams) => onChange({ target: { value: newStreams, name } })} />
                )}
              </Field>
            </Col>
          </TopRow>

          <Row className="no-bm">
            <Col md={12}>
              <div className="pull-right search-help">
                <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                   title="Search query syntax documentation"
                                   text={<Icon name="lightbulb" type="regular" />} />
              </div>
              <SearchButton running={isSubmitting}
                            disabled={isGloballyOverridden || isSubmitting || !isValid}
                            dirty={dirty} />

              <Field name="queryString">
                {({ field: { name, value, onChange } }) => (
                  <QueryInput value={value}
                              disabled={isGloballyOverridden}
                              placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                              onChange={(newQuery) => {
                                onChange({ target: { value: newQuery, name } });

                                return Promise.resolve();
                              }}
                              onExecute={handleSubmit} />
                )}
              </Field>
            </Col>
          </Row>
        </>
      </Wrapper>
    </>
  );
};

WidgetQueryControls.propTypes = {};

export default connect(
  WidgetQueryControls,
  {
    availableStreams: StreamsStore,
    configurations: SearchConfigStore,
    globalOverride: GlobalOverrideStore,
  },
  ({ availableStreams: { streams = [] }, configurations, ...rest }) => ({
    ...rest,
    availableStreams: streams.map((stream) => ({ key: stream.title, value: stream.id })),
    config: configurations.searchesClusterConfig,
  }),
);
