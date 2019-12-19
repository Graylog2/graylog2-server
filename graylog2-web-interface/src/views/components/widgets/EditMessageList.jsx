// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Row, Col, Checkbox } from 'components/graylog';
import * as Immutable from 'immutable';

import SortableSelect from 'views/components/aggregationbuilder/SortableSelect';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { defaultCompare } from 'views/logic/DefaultCompare';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import DescriptionBox from 'views/components/aggregationbuilder/DescriptionBox';
import DecoratorSidebar from 'views/components/messagelist/decorators/DecoratorSidebar';

const ListWrapper = styled.div`
  height: 100%;
  padding-bottom: 25px;
`;

const ValueComponent = styled.span`
  padding: 2px 5px;
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

type Props = {
  children: React.Node,
  config: MessagesWidgetConfig,
  fields: Immutable.Set<FieldTypeMapping>,
  onChange: (MessagesWidgetConfig) => void,
};

const EditMessageList = ({ children, config, fields, onChange }: Props) => {
  const fieldsForSelect = fields
    .map(fieldType => fieldType.name)
    .map(fieldName => ({ label: fieldName, value: fieldName }))
    .valueSeq()
    .toJS()
    .sort((v1, v2) => defaultCompare(v1.label, v2.label));
  const selectedFieldsForSelect = config.fields.map(fieldName => ({ field: fieldName }));

  const onDecoratorsChange = newDecorators => onChange(config.toBuilder().decorators(newDecorators).build());

  return (
    <Row style={{ height: '100%' }}>
      <Col md={3}>
        <DescriptionBox description="Fields">
          <SortableSelect options={fieldsForSelect}
                          onChange={newFields => _onFieldSelectionChanged(newFields, config, onChange)}
                          valueComponent={({ children: _children }) => <ValueComponent>{_children}</ValueComponent>}
                          value={selectedFieldsForSelect} />
          <Checkbox checked={config.showMessageRow} onChange={() => _onShowMessageRowChanged(config, onChange)}>
            Show message in new row
          </Checkbox>
        </DescriptionBox>
        <DescriptionBox description="Decorators">
          <DecoratorSidebar stream="000000000000000000000001"
                            decorators={config.decorators}
                            maximumHeight={600}
                            onChange={onDecoratorsChange} />
        </DescriptionBox>
      </Col>
      <Col md={9} style={{ height: '100%' }}>
        <ListWrapper>
          {children}
        </ListWrapper>
      </Col>
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
