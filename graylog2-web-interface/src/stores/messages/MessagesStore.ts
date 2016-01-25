/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

const moment = require('moment');

import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;
const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
const MessageFieldsFilter = require('logic/message/MessageFieldsFilter');

interface Field {
    name: string;
    value: string;
}

interface Message {
    id: string;
    index: number;
    fields: Array<Field>;
}

var MessagesStore = {
    loadMessage(index: string, messageId: string): Promise<Message> {
        var url = jsRoutes.controllers.MessagesController.single(index.trim(), messageId.trim()).url;
        const promise = fetch('GET', URLUtils.qualifyUrl(url))
            .then(response => {
                const message = response.message;
                const fields = message.fields;
                const filteredFields = MessageFieldsFilter.filterFields(fields);
                const newMessage = {
                    id: message.id,
                    timestamp: moment(message.timestamp).unix(),
                    filtered_fields: filteredFields,
                    formatted_fields: filteredFields,
                    fields: fields,
                    index: response.index,
                    source_node_id: fields.gl2_source_node,
                    source_input_id: fields.gl2_source_input,
                    stream_ids: message.streams,
                };

                return newMessage;
            })
            .catch(errorThrown => {
                UserNotification.error("Loading message information failed with status: " + errorThrown,
                    "Could not load message information");
            });
        return promise;
    },

    fieldTerms(index: string, string: string): Promise<Array<string>> {
        const url = jsRoutes.controllers.MessagesController.analyze(index, string).url;
        const promise = fetch('GET', URLUtils.qualifyUrl(url))
            .then((response) => response.tokens)
            .catch((error) => {
                UserNotification.error("Loading field terms failed with status: " + error,
                    "Could not load field terms.");
            });
        return promise;
    }
};

export = MessagesStore;
