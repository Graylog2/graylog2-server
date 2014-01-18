package org.graylog2.inputs;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.notifications.Notification;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.system.activities.Activity;

import java.util.List;
import java.util.Map;

public class ServerInputRegistry extends InputRegistry {
    protected final Core _core;
    public ServerInputRegistry(Core core) {
        super(core);
        this._core = core;
    }

    public static MessageInput getMessageInput(Input io, Core core) throws NoSuchInputTypeException, ConfigurationException {
        MessageInput input = InputRegistry.factory(io.getType());

        // Add all standard fields.
        input.initialize(new Configuration(io.getConfiguration()), core);
        input.setTitle(io.getTitle());
        input.setCreatorUserId(io.getCreatorUserId());
        input.setPersistId(io.getId());
        input.setCreatedAt(io.getCreatedAt());
        if (io.isGlobal())
            input.setGlobal(true);

        // Add extractors.
        for (Extractor extractor : io.getExtractors()) {
            input.addExtractor(extractor.getId(), extractor);
        }

        // Add static fields.
        for (Map.Entry<String, String> field : io.getStaticFields().entrySet()) {
            input.addStaticField(field.getKey(), field.getValue());
        }

        input.checkConfiguration();

        return input;
    }

    protected List<MessageInput> getAllPersisted() {
        List<MessageInput> result = Lists.newArrayList();

        for (Input io : Input.allOfThisNode(_core)) {
            MessageInput input = null;
            try {
                input = getMessageInput(io, _core);
                result.add(input);
            } catch (NoSuchInputTypeException e) {
                LOG.warn("Cannot launch persisted input. No such type [{}].", io.getType());
            } catch (ConfigurationException e) {
                LOG.error("Missing or invalid input plugin configuration.", e);
            }
        }

        return result;
    }

    public void cleanInput(MessageInput input) {
        // Remove in Mongo.
        Input.destroy(new BasicDBObject("_id", new ObjectId(input.getPersistId())), _core, Input.COLLECTION);
    }

    @Override
    protected void finishedLaunch(InputState state) {
        switch (state.getState()) {
            case RUNNING: Notification.fixed(_core, Notification.Type.NO_INPUT_RUNNING);
            case FAILED:
                _core.getActivityWriter().write(new Activity(state.getDetailedMessage(), InputRegistry.class));
                Notification notification = Notification.buildNow(_core);
                notification.addType(Notification.Type.INPUT_FAILED_TO_START).addSeverity(Notification.Severity.NORMAL);
                notification.addThisNode();
                notification.addDetail("input_id", state.getMessageInput().getId());
                notification.addDetail("reason", state.getDetailedMessage());
                notification.publish();
        }
    }

    @Override
    protected void finishedTermination(InputState state) {
    }
}