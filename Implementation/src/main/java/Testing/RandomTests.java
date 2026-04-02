package Testing;

import Persistence.SqliteRoomPersistence;
import Rooms.Room;

import java.nio.file.Path;
import java.sql.SQLException;

public class RandomTests {
    public static void main(String[] args) {
        Path db = Path.of("data", "database.db");
        SqliteRoomPersistence roomDb = new SqliteRoomPersistence(db);

        try {
            for (Room r : roomDb.findAll()) {
                System.out.println(r.toString());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
