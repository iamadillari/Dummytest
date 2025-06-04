public void validateClientEncryption(String clientId, List<String> keyType) throws SQLException {
    String respClientId = "";
    String respKeyType = "";
    try (Connection conn = PdmDbDsManager.getInstance().getDataSource(rdrConfig).getConnection()) {
        PreparedStatement ps = conn.prepareStatement(SELECT_CLNT_ENCRYPT_DTL_SQL);
        ps.setString(parameterIndex: 1, clientId);

        // Use Awaitility to poll for the database entry
        ResultSet[] resultSet = new ResultSet[1]; // Array to hold the ResultSet for use in lambda
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS) // Maximum wait time of 30 seconds
                .pollInterval(2, TimeUnit.SECONDS) // Poll every 2 seconds
                .until(() -> {
                    resultSet[0] = ps.executeQuery(); // Execute the query
                    return resultSet[0].next(); // Return true if a record is found
                });

        // If we reach here, the data is found
        respClientId = resultSet[0].getString(columnLabel: "CLNT_ID");
        AonAssert.assertEquals(respClientId, clientId,
                message: "Validating Encryption Details for the onboarded clientId in the " +
                        "Client Encryption Details table");
        respKeyType = resultSet[0].getString(columnLabel: "KEY_TYPE");
        AonAssert.assertTrue(keyType.contains(respKeyType),
                message: "Validating Key Status in the 'Client Encryption Details'");
        Logger.info("Expected Encryption key types: {} found for clientId: {} in the 'Client Encryption Details' table",
                keyType, clientId);
    }
}



-------


try {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .until(() -> {
            resultSet[0] = ps.executeQuery();
            return resultSet[0].next();
        });
} catch (org.awaitility.core.ConditionTimeoutException e) {
    throw new SQLException("TIMEOUT: Entry not found in database after waiting for 30 seconds", e);
}


-------


using jdk 8



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public void validateClientEncryption(String clientId, java.util.List<String> keyType) throws SQLException {
    String respClientId = "";
    String respKeyType = "";
    try (Connection conn = PdmDbDsManager.getInstance().getDataSource(rdrConfig).getConnection()) {
        PreparedStatement ps = conn.prepareStatement(SELECT_CLNT_ENCRYPT_DTL_SQL);
        ps.setString(1, clientId); // Parameter index starts at 1

        // Polling for the database entry using JDK 8 compatible approach
        ResultSet resultSet = ps.executeQuery();
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 30000; // 30 seconds max wait time
        long waitInterval = 2000; // Wait 2 seconds between retries
        while (!resultSet.next()) {
            // Check if we've exceeded the maximum wait time
            if (System.currentTimeMillis() - startTime > maxWaitTime) {
                throw new SQLException("TIMEOUT: Entry not found in database after waiting for " + maxWaitTime + "ms");
            }
            // Wait for a short interval before retry
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            resultSet = ps.executeQuery(); // Retry the query
        }

        // If we reach here, the data is found
        respClientId = resultSet.getString("CLNT_ID");
        AonAssert.assertEquals(respClientId, clientId,
                "Validating Encryption Details for the onboarded clientId in the Client Encryption Details table");
        respKeyType = resultSet.getString("KEY_TYPE");
        AonAssert.assertTrue(keyType.contains(respKeyType),
                "Validating Key Status in the 'Client Encryption Details'");
        Logger.info("Expected Encryption key types: {} found for clientId: {} in the 'Client Encryption Details' table",
                keyType, clientId);
    }
}
