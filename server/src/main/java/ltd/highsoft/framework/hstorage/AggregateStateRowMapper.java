package ltd.highsoft.framework.hstorage;

import org.springframework.jdbc.core.RowMapper;

import java.sql.*;

public class AggregateStateRowMapper implements RowMapper<AggregateState> {

    private final String collection;

    public AggregateStateRowMapper(String collection) {
        this.collection = collection;
    }

    @Override
    public AggregateState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AggregateState(
            collection, rs.getString("id"), rs.getString("state"), rs.getTimestamp("timestamp").toInstant()
        );
    }

}
