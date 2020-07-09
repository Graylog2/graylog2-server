// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import * as Immutable from 'immutable';

import { Row, Col, Checkbox } from 'components/graylog';
import FieldSelect from 'views/components/widgets/FieldSelect';
import CustomPropTypes from 'views/components/CustomPropTypes';
import FieldSortSelect from 'views/components/widgets/FieldSortSelect';
import SortDirectionSelect from 'views/components/widgets/SortDirectionSelect';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import DescriptionBox from 'views/components/aggregationbuilder/DescriptionBox';
import DecoratorSidebar from 'views/components/messagelist/decorators/DecoratorSidebar';

const FullHeightCol = styled(Col)`
  height: 100%;
  padding-bottom: 10px;
  overflow: auto;
`;

const _onFieldSelectionChanged = (fields, config, onChange) => {
  const newFields = fields.map(({ value }) => value);
  const newConfig = config.toBuilder().fields(newFields).build();

  return onChange(newConfig);
};

const _onShowMessageRowChanged = (config, onChange) => {
  const newConfig = config.toBuilder().showMessageRow(!config.showMessageRow).build();

  return onChange(newConfig);
};

const _onSortChange = (sort: $PropertyType<AggregationWidgetConfig, 'sort'>, config, onChange) => {
  const newConfig = config.toBuilder().sort(sort).build();

  return onChange(newConfig);
};

const _onSortDirectionChange = (direction: $PropertyType<AggregationWidgetConfig, 'direction'>, config, onChange) => {
  const newConfig = config.toBuilder().sort(config.sort.map((sort) => sort.toBuilder().direction(direction).build())).build();

  return onChange(newConfig);
};

type Props = {
  children: React.Node,
  config: MessagesWidgetConfig,
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (MessagesWidgetConfig) => void,
};

const EditMessageList = ({ children, config, fields, onChange }: Props) => {
  const { sort } = config;
  const [sortDirection] = (sort || []).map((s) => s.direction);
  const selectedFieldsForSelect = config.fields.map((fieldName) => ({ field: fieldName }));

  const onDecoratorsChange = (newDecorators) => onChange(config.toBuilder().decorators(newDecorators).build());

  return (
    <Row style={{ height: '100%', paddingBottom: '15px' }}>
      <FullHeightCol md={3}>
        <DescriptionBox description="Fields">
          <FieldSelect fields={fields}
                       onChange={(newFields) => _onFieldSelectionChanged(newFields, config, onChange)}
                       value={selectedFieldsForSelect} />
          <Checkbox checked={config.showMessageRow} onChange={() => _onShowMessageRowChanged(config, onChange)}>
            Show message in new row
          </Checkbox>
        </DescriptionBox>
        <DescriptionBox description="Sorting">
          <FieldSortSelect fields={fields} sort={sort} onChange={(data) => _onSortChange(data, config, onChange)} />
        </DescriptionBox>
        <DescriptionBox description="Direction">
          <SortDirectionSelect disabled={!sort || sort.length === 0}
                               direction={sortDirection && sortDirection.direction}
                               onChange={(data) => _onSortDirectionChange(data, config, onChange)} />
        </DescriptionBox>
        <DescriptionBox description="Decorators">
          <DecoratorSidebar stream="000000000000000000000001"
                            decorators={config.decorators}
                            maximumHeight={600}
                            onChange={onDecoratorsChange} />
        </DescriptionBox>
      </FullHeightCol>
      <FullHeightCol md={9}>
        {children}
      </FullHeightCol>
    </Row>
  );
};

EditMessageList.propTypes = {
  children: CustomPropTypes.OneOrMoreChildren.isRequired,
  config: PropTypes.object.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default EditMessageList;
