package org.graylog2.inputs.random.generators;

import com.google.common.collect.ImmutableList;
import org.graylog2.inputs.random.generators.states.Country;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.Random;

import static org.graylog2.inputs.random.generators.states.Country.COUNTRIES;

public class FakeEventMessageGenerator extends FakeMessageGenerator {
    private static final ImmutableList<Category> CATEGORIES = ImmutableList.of(
            new Category("100000", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon", 8),
            new Category("100001", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon success", 10),
            new Category("100002", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon failure", 2),
            new Category("100003", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon with alternate credentials", 1),
            new Category("100004", GeneratorState.CategoryName.AUTHENTICATION, "logon", "session reconnect", 4),
            new Category("102500", GeneratorState.CategoryName.AUTHENTICATION, "logoff", "logoff", 4),
            new Category("102501", GeneratorState.CategoryName.AUTHENTICATION, "logoff", "session disconnect", 4),
            new Category("160500", GeneratorState.CategoryName.ENDPOINT, "ports", "port open", 2),
            new Category("160501", GeneratorState.CategoryName.ENDPOINT, "ports", "port closed", 2),
            new Category("160502", GeneratorState.CategoryName.ENDPOINT, "ports", "open ports", 2)
    );

    private static final ImmutableList<User> USERS = ImmutableList.of(
            new User("rand", "1001", "black-tower", 5),
            new User("mat", "1002",  "red-hand", 3),
            new User("perrin", "1003",  "edmonds-field", 3),
            new User("egwene", "1004",  "white-tower", 3),
            new User("nynaeve", "1005",  "white-tower", 3),
            new User("moraine", "1006",  "white-tower", 4),
            new User("elayne", "1006",  "caemlyn", 4)
    );

    private static final ImmutableList<Application> APPLICATIONS = ImmutableList.of(
            new Application("LDAP", 10),
            new Application("RADIUS", 4),
            new Application("Active Directory", 8)
    );

    private static final ImmutableList<Reference> REFERENCES = ImmutableList.of(
            new Reference("edmonds-field.acme.org", 10),
            new Reference("tar-valon.acme.org", 10),
            new Reference("caemlyn.acme.org", 10),
            new Reference("tear.acme.org", 10),
            new Reference("illian.acme.org", 10),
            new Reference("mayene.acme.org", 10),
            new Reference("black-tower.acme.org", 10),
            new Reference("four-kings.acme.org", 10)
    );

    private static final ImmutableList<EventSource> EVENT_SOURCES = ImmutableList.of(
            new EventSource("firefox", 10),
            new EventSource("chrome", 8),
            new EventSource("irssi", 5),
            new EventSource("thunderbrid", 2)
    );

    public FakeEventMessageGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public GeneratorState generateState() {
        final GeneratorState generatorState = new GeneratorState();
        final int successProb = RANDOM.nextInt(100);

        generatorState.isSuccessful = successProb < 97;
        generatorState.msgSequenceNr = msgSequenceNumber++;
        generatorState.source = source;
        generatorState.eventTypeCode = ((Category) getWeighted(CATEGORIES)).gl2EventTypeCode;
        generatorState.userName = ((User) getWeighted(USERS)).userName;
        generatorState.applicationName = ((Application) getWeighted(APPLICATIONS)).applicationName;
        generatorState.destinationRef = ((Reference) getWeighted(REFERENCES)).referenceName;
        generatorState.sourceRef = ((Reference) getWeighted(REFERENCES)).referenceName;
        generatorState.eventSource = ((EventSource) getWeighted(EVENT_SOURCES)).eventSource;
        generatorState.country = ((Country) getWeighted(COUNTRIES)).getCountry();

        return generatorState;
    }

    private static Message createMessage(GeneratorState state, Category category, String shortMessage) {
        final Message msg = new Message(shortMessage, state.source, Tools.nowUTC());
        final Country country = COUNTRIES.stream().filter(c -> c.getCountry().equals(state.country)).findFirst().orElseThrow(() -> new RuntimeException("Could not find country: " + state.country));
        msg.addField("sequence_nr", state.msgSequenceNr);
        msg.addField("gl2_event_type_code", category.gl2EventTypeCode);
        msg.addField("gl2_event_category", category.gl2EventCategory);
        msg.addField("gl2_event_subcategory", category.gl2EventSubcategory);
        msg.addField("gl2_event_type", category.gl2EventType);
        msg.addField("country", country.getCountry());
        msg.addField("city", country.getCapital());
        msg.addField("geolocation", country.getGeolocation());

        return msg;
    }

    public static Message generateMessage(GeneratorState state) {
        final Category category = CATEGORIES.stream().filter(c -> c.gl2EventTypeCode.equals(state.eventTypeCode)).findFirst().orElseThrow(() -> new RuntimeException("Could not find category"));

        switch (category.gl2EventCategory) {
            case AUTHENTICATION: return simulateAuthentication(state, RANDOM);
            case ENDPOINT: return simulateEndpoint(state, RANDOM);
            default: throw new RuntimeException("Unknown Category" + category.gl2EventCategory);
        }
    }

    public static Message simulateEndpoint(GeneratorState state, Random rand) {
        final String shortMessage = getShortMessage(state);
        final Category category = CATEGORIES.stream().filter(c -> c.gl2EventTypeCode.equals(state.eventTypeCode)).findFirst().orElseThrow(() -> new RuntimeException("Could not find category"));
        final Message msg = createMessage(state, category, shortMessage);

        msg.addField("event_source", state.eventSource);
        msg.addField("user_name", state.userName);
        msg.addField("process_id", rand.nextInt(10000));
        msg.addField("process_command_line", "/usr/bin/" + state.eventSource);

        return msg;
    }

    public static String getShortMessage(GeneratorState state) {
        switch (state.eventTypeCode) {
            case "100000": return DateTime.now() + ": User " + state.userName + " is logging on " + state.destinationRef;
            case "100001": return DateTime.now() + ": User " + state.userName + " is successfully logging on " + state.destinationRef;
            case "100002": return DateTime.now() + ": User " + state.userName + " failed to logon " + state.destinationRef;
            case "100003": return DateTime.now() + ": User " + state.userName + " is logging on with alternate credentials " + state.destinationRef;
            case "100004": return DateTime.now() + ": User " + state.userName + " the session was reconnected to " + state.destinationRef;
            case "102500": return DateTime.now() + ": User " + state.userName + " has logged of from " + state.destinationRef;
            case "102501": return DateTime.now() + ": User " + state.userName + " was disconnected from" + state.destinationRef;
            case "160500": return DateTime.now() + ": User " + state.userName + " opened port for " + state.eventSource;
            case "160501": return DateTime.now() + ": User " + state.userName + " closed port for " + state.eventSource;
            case "160502": return DateTime.now() + ": User " + state.userName + " has open ports for " + state.eventSource;
            default: return "unknown event type";
        }
    }

    public static Message authenticationLogonFields(Message msg, GeneratorState state) {
        final Optional<User> user = USERS.stream().filter(u -> u.userName.equals(state.userName)).findFirst();
        msg.addField("application_name", state.applicationName);
        msg.addField("destination_reference", state.destinationRef);
        msg.addField("source_reference", state.sourceRef);
        msg.addField("event_outcome", state.isSuccessful);
        user.ifPresent(u -> msg.addField("user_domain", u.userDomain));
        msg.addField("user_name", state.userName);

        return msg;
    }

    public static Message authenticationLogoffFields(Message msg, GeneratorState state) {
        final Optional<User> user = USERS.stream().filter(u -> u.userName.equals(state.userName)).findFirst();
        msg.addField("application_name", state.applicationName);
        msg.addField("source_reference", state.sourceRef);
        user.ifPresent(u -> msg.addField("user_domain", u.userDomain));
        msg.addField("user_name", state.userName);

        return msg;
    }

    public static Message simulateAuthentication(GeneratorState state, Random rand) {
        final String shortMessage = getShortMessage(state);
        final Category category = CATEGORIES.stream().filter(c -> c.gl2EventTypeCode.equals(state.eventTypeCode)).findFirst().orElseThrow(() -> new RuntimeException("Could not find category"));
        final Message msg = createMessage(state, category, shortMessage);

        switch (category.gl2EventSubcategory) {
            case "logon": return authenticationLogonFields(msg, state);
            case "logoff": return authenticationLogoffFields(msg, state);
            default: return msg;
        }
    }

    public static class GeneratorState extends FakeMessageGenerator.GeneratorState {
        public long msgSequenceNr;
        public boolean isSuccessful;
        public String source;
        public String eventTypeCode;
        public String userName;
        public String applicationName;
        public String destinationRef;
        public String sourceRef;
        public String eventSource;
        public String country;

        public enum CategoryName {
            AUTHENTICATION, ENDPOINT,
        }
    }

    private static class Application extends Weighted {
        private final String applicationName;

        private Application(String applicationName, int weight) {
            super(weight);

            this.applicationName = applicationName;
        }

        public String getApplicationName() {
            return applicationName;
        }
    }

    private static class EventSource extends Weighted {
        private final String eventSource;

        private EventSource(String eventSource, int weight) {
            super(weight);

            this.eventSource = eventSource;
        }

        public String getApplicationName() {
            return eventSource;
        }
    }

    private static class Reference extends Weighted {
        private final String referenceName;

        private Reference(String refernceName, int weight) {
            super(weight);

            this.referenceName = refernceName;
        }

        public String getReferenceName() {
            return referenceName;
        }
    }

    private static class User extends Weighted {
        private final String userName;
        private final String userId;
        private final String userDomain;


        private User(String userName, String userId, String userDomain, int weight) {
            super(weight);

            this.userName = userName;
            this.userId = userId;
            this.userDomain = userDomain;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserDomain() {
            return userDomain;
        }
    }

    private static class Category extends Weighted {
       private final String gl2EventTypeCode;
       private final GeneratorState.CategoryName gl2EventCategory;
       private final String gl2EventSubcategory;
       private final String gl2EventType;

        public Category(String gl2EventTypeCode, GeneratorState.CategoryName gl2EventCategory, String gl2EventSubcategory, String gl2EventType, int weight) {
            super(weight);

            this.gl2EventTypeCode = gl2EventTypeCode;
            this.gl2EventCategory = gl2EventCategory;
            this.gl2EventSubcategory = gl2EventSubcategory;
            this.gl2EventType = gl2EventType;
        }

        public String getTypeCode() {
           return gl2EventTypeCode;
        }

        public GeneratorState.CategoryName getCategory() {
            return gl2EventCategory;
        }

        public String getSubcategory() {
            return gl2EventSubcategory;
        }

        public String getType() {
            return gl2EventType;
        }
    }

}
