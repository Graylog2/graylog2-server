import React from 'react';

import { Row, Col } from 'components/bootstrap';
import { LookupTableForm } from 'components/lookup-tables';

type LookupTableCreateProps = {
  saved: (...args: any[]) => void;
  validate?: (...args: any[]) => void;
  validationErrors?: any;
};

class LookupTableCreate extends React.Component<LookupTableCreateProps, {
  [key: string]: any;
}> {
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
