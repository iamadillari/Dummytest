public void validateClientOnboarding(String clientId, String operationStatus) throws SQLException {
    String respClientId = "";
    String respClientStatus = "";
    boolean present = false;

    try (Connection conn = PdmDbDsManager.getInstance().getDataSource(rdrConfig).getConnection()) {
        PreparedStatement ps = conn.prepareStatement(SELECT_CLNT_DTL_SQL());
        
        ps.setString(parameterIndex: 1, clientId);
        SQLWait.executeSQLPsQuery(ps);
        
        // Wait before the first query attempt to give the DB time to update
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 30000; // 30 seconds max wait time
        long waitInterval = 2000; // Wait 2 seconds between retries

        ResultSet resultSet = ps.executeQuery();
        while (!resultSet.next()) {
            // Check if we've exceeded the maximum wait time
            if (System.currentTimeMillis() - startTime > maxWaitTime) {
                throw new SQLException("Timeout: Entry not found in database after waiting for " + maxWaitTime + "ms");
            }

            // Wait for a short interval before retrying
            Thread.sleep(waitInterval);
            resultSet = ps.executeQuery(); // Retry the query
        }

        // If we reach here, the data is found
        respClientId = resultSet.getString(columnLabel: "CLNT_ID");
        respClientStatus = resultSet.getString(columnLabel: "CLNT_STAT");

    }

    Assert.assertEquals(respClientId, clientId, message: "Validating entry in the 'Client Details' Table for the Onboarded ClientId");
    Assert.assertTrue(respClientStatus.contains(operationStatus), message: "Validating Operation Status in the 'Client Details' Table");
    Logger.info("OPERATION STATUS: {} found for clientId: {} in 'Client Details' table", operationStatus, clientId);
}