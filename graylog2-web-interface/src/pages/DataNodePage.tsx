import React from 'react'
import useParams from "routing/useParams";
import useDataNodes from "components/datanode/hooks/useDataNodes";
import DataNodesPageNavigation from "components/datanode/DataNodePageNavigation";
import DocsHelper from "util/DocsHelper";
import { Row, Col } from 'components/bootstrap';
import {DocumentTitle, PageHeader, Spinner} from 'components/common';
const DataNodePage = () => {
  const { dataNodeId} = useParams();
  const { data: { elements }, isInitialLoading } = useDataNodes({
    query: '',
    page: 1,
    pageSize: 0,
  });

  if (isInitialLoading) {
    return <Spinner />
  }
  console.log(elements);

  return (
    <DocumentTitle title="Data Nodes Migration">
      <DataNodesPageNavigation />
      <PageHeader title="Data Nodes Migration"
                  documentationLink={{
                    title: 'Data Nodes documentation',
                    path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
                  }}>
      <span>
        Graylog data nodes offer a better integration with Graylog and simplify future updates. They allow you to index and search through all the messages in your Graylog message database.
      </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>

        </Col>
      </Row>
    </DocumentTitle>
  )
}
export default DataNodePage
