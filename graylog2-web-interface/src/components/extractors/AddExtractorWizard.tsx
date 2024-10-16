import React from 'react';

import { Row, Col, Button } from 'components/bootstrap';
import LoaderTabs from 'components/messageloaders/LoaderTabs';
import MessageFieldExtractorActions from 'components/search/MessageFieldExtractorActions';

type AddExtractorWizardProps = {
  inputId?: string;
};

class AddExtractorWizard extends React.Component<AddExtractorWizardProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    inputId: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      showExtractorForm: false,
    };
  }

  _showAddExtractorForm = () => {
    this.setState(({ showExtractorForm }) => ({ showExtractorForm: !showExtractorForm }));
  };

  render() {
    let extractorForm;

    if (this.state.showExtractorForm) {
      // Components using this component, will give it a proper fieldName and message
      const extractorFieldActions = <MessageFieldExtractorActions fieldName="" message={{}} />;

      extractorForm = (
        <div className="stream-loader">
          <LoaderTabs selectedInputId={this.props.inputId} customFieldActions={extractorFieldActions} />
        </div>
      );
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2 style={{ marginBottom: 5 }}>Add extractor</h2>

          <p>
            Start by loading a message to have an example to work on. You can decide whether to load a recent message
            received by this input, or manually select a message giving its ID.
          </p>
          <p>
            <Button bsStyle="success" bsSize="small" onClick={this._showAddExtractorForm} disabled={this.state.showExtractorForm}>
              Create extractor
            </Button>
          </p>

          {extractorForm}
        </Col>
      </Row>
    );
  }
}

export default AddExtractorWizard;
