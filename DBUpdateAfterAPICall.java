import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.time.Duration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class RestAssuredWithDatabaseWaitTest {

    private static final String DB_URL = "jdbc:your_db_url";
    private static final String DB_USER = "your_db_user";
    private static final String DB_PASS = "your_db_password";

    @Test
    void testDatabaseUpdateAfterApiCall() {
        // Assume this is the API call that updates the database
        // Replace this with your actual API call using Rest Assured
        String apiResponse = performApiCallAndGetResult();

        // Wait for the database update to complete
        await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> {
                    try {
                        return isDataInDatabase(apiResponse);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Perform assertions on the database
        try {
            // Replace this with your database query logic to verify the data
            assertTrue(isDataInDatabase(apiResponse), "Data should be present in the database");
        } catch (SQLException e) {
            fail("Failed to verify data in database: " + e.getMessage());
        }
    }

    // Helper method to perform the API call (replace with your actual implementation)
    private String performApiCallAndGetResult() {
        // Replace this with your actual API call using Rest Assured
        return "some_api_response";
    }

    // Helper method to check if data is in the database
    private boolean isDataInDatabase(String apiResponse) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM your_table WHERE column = ?");
            preparedStatement.setString(1, apiResponse); // Replace with the data you're checking
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0; // Return true if data is found
            } else {
                return false;
            }
        } finally {
            // Ensure resources are closed in a finally block
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
}
