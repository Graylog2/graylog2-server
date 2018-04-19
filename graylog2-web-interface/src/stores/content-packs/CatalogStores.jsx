import Reflux from 'reflux';
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const CatalogActions = ActionsProvider.getActions('Catalog');

const fetchedEnteties =  [
  {
    "v": "1",
    "type": "input",
    "id": "78547c87-af21-4292-8e57-614da5baf6c3",
    "data": {
      "creator_user_id" : "admin",
      "configuration" : {
        "recv_buffer_size" : 262144,
        "tcp_keepalive" : false,
        "use_null_delimiter" : false,
        "number_worker_threads" : 8,
        "tls_client_auth_cert_file" : "",
        "force_rdns" : false,
        "bind_address" : "0.0.0.0",
        "tls_cert_file" : "",
        "store_full_message" : false,
        "expand_structured_data" : false,
        "port" : 8000,
        "tls_key_file" : "admin",
        "tls_enable" : false,
        "tls_key_password" : "admin",
        "max_message_size" : 2097152,
        "tls_client_auth" : "disabled",
        "override_source" : null,
        "allow_override_date" : true
      },
      "name" : "Syslog TCP",
      "created_at" : "2018-03-22T12:49:32.030Z",
      "global" : false,
      "type" : "org.graylog2.inputs.syslog.tcp.SyslogTCPInput",
      "title" : "hulud.net",
      "content_pack" : null,
      "node_id" : "f3bd37d4-61d2-4157-8d72-8b26d1777c3f",
      "extractors" : [
        {
          "creator_user_id" : "admin",
          "source_field" : "message",
          "condition_type" : "regex",
          "title" : "Command execution",
          "type" : "grok",
          "cursor_strategy" : "copy",
          "target_field" : "",
          "extractor_config" : {
            "grok_pattern" : "\\(%{WORD:login}\\) CMD \\( %{GREEDYDATA:command}\\)"
          },
          "condition_value" : "(.*) CMD",
          "converters" : [],
          "id" : "13b02450-4214-11e8-8ca1-00e18cb9c35a",
          "order" : 0
        },
      ],
    },
  },
  {
    "v": "1",
    "type": "lookup_table",
    "id": "311d9e16-e4d9-485d-a916-337fb4ca0e8b",
    "data": {
      "title": "OTX API - IP",
      "name": "otx-api-ip",
      "cache_id": "911da25d-74e2-4364-b88e-7930368f6e56",
      "data_adapter_id": "2562ac46-65f1-454c-89e1-e9be96bfd5e7"
    }
  },
  {
    "v": "1",
    "type": "lookup_cache",
    "id": "911da25d-74e2-4364-b88e-7930368f6e56",
    "data": {
      "title": "OTX IP Cache",
      "name": "otx-api-ip-cache",
      "config": {
        "type": "guava_cache",
        "max_size": 1000
      }
    }
  },
  {
    "v": "1",
    "type": "lookup_adapter",
    "id": "2562ac46-65f1-454c-89e1-e9be96bfd5e7",
    "data": {
      "title": "OTX IP Adapter",
      "name": "otx-api-ip-adapter",
      "config": {
        "type": "otx-api",
        "api_url": "https://otx.alienvault.com",
        "api_key": "$OTX_API_KEY$"
      }
    }
  },
  {
    "v": "1",
    "type": "pipeline_connection",
    "id": "726b6e09-4199-4ef4-8d69-275ebfe06d31",
    "data": {
      "pipeline_id": "37f26b0a-e4ab-41ff-985c-0dd4a01ff10c",
      "stream_id": "3fb82940-fd01-4c64-98f3-4ed2ce2577e3",
      "config": {
        "subconfig": {
          "key": true,
        }
      }
    }
  },
];

const CatalogStores = Reflux.createStore({
  listenables: [CatalogActions],

  showEntityIndex() {
    const url = URLUtils.qualifyUrl(ApiRoutes.CatalogsController.showEntityIndex().url);
    const promise = fetch('GET', url)
      .then((result) => {
        const entityIndex = lodash.groupBy(result.entities, 'type');
        this.trigger({ entityIndex: entityIndex });

        return result;
      });

    CatalogActions.showEntityIndex.promise(promise);
  },

  getSelectedEntities(ignored) {
    const promise = new Promise(function(resolve, reject) {
      setTimeout(resolve, 100, fetchedEnteties);
    });
    promise.then((entities) => {
      this.trigger({ fetchedEntities: entities });
      return entities;
    });
    CatalogActions.getSelectedEntities.promise(promise);
  },
});

export default CatalogStores;
