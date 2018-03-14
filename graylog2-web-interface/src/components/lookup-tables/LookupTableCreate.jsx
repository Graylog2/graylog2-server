import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col } from 'react-bootstrap';
import { LookupTableForm } from 'components/lookup-tables';

class LookupTableCreate extends React.Component {
  static propTypes = {
    saved: PropTypes.func.isRequired,
    validate: PropTypes.func,
    validationErrors: PropTypes.object,
  };

  static defaultProps = {
    validate: null,
    validationErrors: {},
  };

  state = {
    table: undefined,
  };

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
  }
}

export default LookupTableCreate;
