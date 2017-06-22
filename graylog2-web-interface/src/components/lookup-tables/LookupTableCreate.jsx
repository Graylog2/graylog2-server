import React, { PropTypes } from 'react';
import { Row, Col } from 'react-bootstrap';
import { LookupTableForm } from 'components/lookup-tables';

const LookupTableCreate = React.createClass({

  propTypes: {
    saved: PropTypes.func.isRequired,
    validate: PropTypes.func,
    validationErrors: PropTypes.object,
  },

  getDefaultProps() {
    return {
      validate: null,
      validationErrors: {},
    };
  },

  getInitialState() {
    return {
      table: undefined,
    };
  },

  render() {
    return (
      <div>
        <Row className="content">
          <Col lg={8}>
            <LookupTableForm saved={this.props.saved}
                             create
                             validate={this.props.validate}
                             validationErrors={this.props.validationErrors} />
          </Col>
        </Row>
      </div>
    );
  },

});

export default LookupTableCreate;
