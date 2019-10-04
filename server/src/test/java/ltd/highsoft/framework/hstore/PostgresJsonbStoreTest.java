package ltd.highsoft.framework.hstore;

import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class PostgresJsonbStoreTest {

    private JdbcOperations jdbcTemplate;
    private TimeService timeService;
    private Store store;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(createTestDataSource());
        timeService = new FixedTimeService(Instant.now());
        store = new PostgresJsonbStore(jdbcTemplate, timeService);
        recreateCollectionTable();
    }

    private DataSource createTestDataSource() {
        return new SingleConnectionDataSource(
            "jdbc:postgresql://localhost:5432/hkeeper",
            "postgres", "postgres", false
        );
    }

    @Test
    void should_save_aggregate_state_into_database_correctly() {
        Aggregate aggregate = new Aggregate("0001", "Van");
        store.save(aggregate);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from entities");
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).get("id")).isEqualTo("0001");
        assertThat(rows.get(0).get("state").toString()).isEqualTo("{\"id\": \"0001\", \"name\": \"Van\"}");
        assertThat(rows.get(0).get("timestamp")).isEqualTo(Timestamp.from(timeService.now()));
    }

    @Test
    void should_reject_aggregates_without_id_field() {
        Object object = new TypeWithoutIdField("Van");
        AbstractThrowableAssert<?, ?> exception = assertThatThrownBy(() -> store.save(object));
        exception.isInstanceOf(MappingException.class);
        exception.hasMessage("Missing 'id' field in type 'ltd.highsoft.framework.hstore.TypeWithoutIdField'!");
    }

    @Test
    void should_load_aggregate_state_from_database_correctly() {
        Aggregate aggregate = new Aggregate("0001", "Van");
        store.save(aggregate);
        Aggregate loaded = store.load("0001", Aggregate.class);
        assertThat(loaded).isEqualToComparingFieldByField(aggregate);
    }

    @Test
    void should_throw_aggregate_not_found_exception_while_aggregate_not_found_during_loading() {
        Throwable thrown = catchThrowable(() -> store.load("non-existing-aggregate", Aggregate.class));
        assertThat(thrown).isInstanceOf(AggregateNotFoundException.class);
        assertThat(thrown).hasMessage("Aggregate 'non-existing-aggregate' of type 'ltd.highsoft.framework.hstore.Aggregate' does not exist!");
    }

    private void recreateCollectionTable() {
        jdbcTemplate.execute("drop table if exists entities");
        jdbcTemplate.execute(
            "create table entities\n" +
                "(\n" +
                "    id        varchar(50) primary key,\n" +
                "    state     jsonb       not null,\n" +
                "    timestamp timestamptz not null\n" +
                ")"
        );
    }

}