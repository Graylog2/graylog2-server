package org.graylog.plugins.enterprise.search.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.plugins.database.MongoConnectionRule;
import org.graylog.plugins.enterprise.database.PaginatedList;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ViewServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private ViewService dbService;
    private ClusterConfigServiceImpl clusterConfigService;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        this.clusterConfigService = new ClusterConfigServiceImpl(
                objectMapperProvider,
                mongoRule.getMongoConnection(),
                mock(NodeId.class),
                new ChainingClassLoader(getClass().getClassLoader()),
                new ClusterEventBus()
        );
        this.dbService = new ViewService(mongoRule.getMongoConnection(), objectMapperProvider, clusterConfigService);

    }

    @After
    public void tearDown() {
        mongoRule.getMongoConnection().getMongoDatabase().drop();
    }

    private void hasValidId(ViewDTO dto) {
        assertThat(dto.id())
                .isNotNull()
                .matches("^[a-z0-9]{24}");
    }

    @Test
    public void crud() {
        final ViewDTO dto1 = ViewDTO.builder()
                .title("View 1")
                .summary("This is a nice view")
                .description("This contains lots of descriptions for the view.")
                .build();
        final ViewDTO dto2 = ViewDTO.builder()
                .title("View 2")
                .build();

        final ViewDTO savedDto1 = dbService.save(dto1);
        final ViewDTO savedDto2 = dbService.save(dto2);


        assertThat(savedDto1)
                .satisfies(this::hasValidId)
                .extracting("title", "summary", "description")
                .containsExactly("View 1", "This is a nice view", "This contains lots of descriptions for the view.");

        assertThat(savedDto2)
                .satisfies(this::hasValidId)
                .extracting("title", "summary", "description")
                .containsExactly("View 2", "", "");

        assertThat(dbService.get(savedDto1.id()))
                .isPresent()
                .get()
                .extracting("title")
                .containsExactly("View 1");

        dbService.delete(savedDto2.id());

        assertThat(dbService.get(savedDto2.id())).isNotPresent();
    }

    @Test
    public void searchPaginated() {
        final ImmutableMap<String, SearchQueryField> searchFieldMapping = ImmutableMap.<String, SearchQueryField>builder()
                .put("id", SearchQueryField.create(ViewDTO.FIELD_ID))
                .put("title", SearchQueryField.create(ViewDTO.FIELD_TITLE))
                .put("summary", SearchQueryField.create(ViewDTO.FIELD_DESCRIPTION))
                .build();

        dbService.save(ViewDTO.builder().title("View A").build());
        dbService.save(ViewDTO.builder().title("View B").build());
        dbService.save(ViewDTO.builder().title("View C").build());
        dbService.save(ViewDTO.builder().title("View D").build());
        dbService.save(ViewDTO.builder().title("View E").build());

        final SearchQueryParser queryParser = new SearchQueryParser(ViewDTO.FIELD_TITLE, searchFieldMapping);

        final PaginatedList<ViewDTO> result = dbService.searchPaginated(queryParser.parse("A B D"), "desc", "title", 1, 10);

        assertThat(result)
                .hasSize(3)
                .extracting("title")
                .containsExactly("View D", "View B", "View A");
    }

    @Test
    public void saveAndGetDefault() {
        dbService.save(ViewDTO.builder().title("View A").build());
        final ViewDTO savedView2 = dbService.save(ViewDTO.builder().title("View B").build());

        dbService.saveDefault(savedView2);

        assertThat(dbService.getDefault())
                .isPresent()
                .get()
                .extracting("id", "title")
                .containsExactly(savedView2.id(), "View B");

        assertThatThrownBy(() -> dbService.saveDefault(ViewDTO.builder().title("err").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}