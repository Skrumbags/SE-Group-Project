package Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Applies {@code /schema.sql} once per connection (idempotent {@code CREATE IF NOT EXISTS}).
 */
public final class SchemaInstaller {

    private SchemaInstaller() {
    }

    public static void apply(Connection conn) throws SQLException, IOException {
        try (InputStream in = SchemaInstaller.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath");
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement st = conn.createStatement()) {
                for (String part : sql.split(";")) {
                    String stmt = part.trim();
                    if (!stmt.isEmpty()) {
                        st.executeUpdate(stmt);
                    }
                }
            }
        }
    }
}
