import org.awaitility.Awaitility;

import java.sql.*;
import java.time.Duration;
import java.util.concurrent.Callable;

public class DbWaitUtil {

    private static final int TIMEOUT_SECONDS = 30;
    private static final int POLL_INTERVAL_MILLIS = 1000;

    /**
     * Waits for a specific row to be available in the database.
     *
     * @param conn            JDBC Connection
     * @param sql             SQL query (e.g., SELECT * FROM table WHERE ...)
     * @param param           Parameter to set in PreparedStatement
     * @param columnLabel     Column name to check (e.g., "CLNT_STAT")
     * @param expectedValue   Expected value (e.g., "ONBOARDED")
     */
    public static void waitForRecord(Connection conn, String sql, String param, String columnLabel, String expectedValue) {
        Awaitility.await()
            .atMost(Duration.ofSeconds(TIMEOUT_SECONDS))
            .pollInterval(Duration.ofMillis(POLL_INTERVAL_MILLIS))
            .until(() -> recordExists(conn, sql, param, columnLabel, expectedValue));
    }

    private static boolean recordExists(Connection conn, String sql, String param, String columnLabel, String expectedValue) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String value = rs.getString(columnLabel);
                if (expectedValue.equalsIgnoreCase(value)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // Optional: log exception
            e.printStackTrace();
        }
        return false;
    }
}