package ltd.highsoft.hkeeper;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Types;
import java.time.Instant;

public class PostgresJsonbStore extends Store {
    private final JdbcOperations jdbcTemplate;
    private final ObjectMapper mapper;

    PostgresJsonbStore(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = createMapper();
    }

    private ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return mapper;
    }

    @Override
    public void save(Object entity) {
        saveSate(new EntityState(extractId(entity), asContent(entity), Instant.now()));
    }

    private Object extractId(Object entity) {
        return ReflectionTestUtils.getField(entity, "id");
    }

    private String asContent(Object entity) {
        try {
            return mapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Mapping error: ", e);
        }
    }

    private void saveSate(EntityState state) {
        String sql = "insert into entities (id, state, timestamp) values (?, ?, ?)";
        jdbcTemplate.execute(sql, (PreparedStatementCallback<Boolean>) ps -> {
            ps.setObject(1, state.id());
            ps.setObject(2, state.content(), Types.OTHER);
            ps.setObject(3, state.timestamp());
            return ps.execute();
        });
    }

}
