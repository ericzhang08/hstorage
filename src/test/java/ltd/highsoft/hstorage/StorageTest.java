package ltd.highsoft.hstorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StorageTest {

    private static final String COLLECTION = "test_aggregates";
    private TestDatabase testDatabase;
    private TimeService timeService;
    private Storage storage;

    @BeforeEach
    void setUp() {
        testDatabase = new TestDatabase();
        testDatabase.recreateCollectionTable(COLLECTION);
        timeService = new FixedTimeService(Instant.now());
        storage = new Storage(configureMapping(), new JdbcStatePersister(testDatabase.jdbcTemplate()), timeService);
    }

    private ModelMapping configureMapping() {
        MappingConfigurer mappingConfigurer = new MappingConfigurer();
        mappingConfigurer.addPackage("ltd.highsoft.hstorage.test");
        return mappingConfigurer.configure();
    }

    @Test
    void should_be_able_to_save_aggregate_state_into_database() {
        TestAggregate testAggregate = new TestAggregate("0001", "Van");
        storage.save(testAggregate);
        Map<String, Object> loaded = testDatabase.getSavedAggregateState(COLLECTION);
        assertThat(loaded.get("id")).isEqualTo("0001");
        assertThat(loaded.get("state").toString()).isEqualTo("{\"id\": \"0001\", \"name\": \"Van\", \"@type\": \"aggregate\"}");
        assertThat(loaded.get("timestamp")).isEqualTo(Timestamp.from(timeService.now()));
    }

    @Test
    void should_be_able_to_load_aggregate_state_from_database() {
        TestAggregate testAggregate = new TestAggregate("0001", "Van");
        storage.save(testAggregate);
        TestAggregate loaded = storage.load("0001", TestAggregate.class);
        assertThat(loaded).isEqualToComparingFieldByField(testAggregate);
    }

}
