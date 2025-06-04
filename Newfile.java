public void validateClientOnboarding(String clientId, String operationsStatus) throws SQLException {
    try (Connection conn = PdnDDBsManager.getInstance().getDataSource(rdrConfig).getConnection();
         PreparedStatement ps = conn.prepareStatement(SELECT_CLMT_DTL_SQL)) {
        
        ps.setString(1, clientId);
        
        // Wait for data to appear (max 30 seconds)
        ResultSet resultSet = waitForClientData(ps, 30000, 2000);
        
        // Validate all entries
        boolean found = false;
        do {
            String dbClientId = resultSet.getString("CLMT_ID");
            String dbClientStatus = resultSet.getString("CLMT_STAT");
            
            // Validate each record that matches our clientId
            if (clientId.equals(dbClientId)) {
                found = true;
                AonAssert.assertEquals(dbClientId, clientId, 
                    "Validating entry in the 'Client Details' Table for the Onboarded ClientId");
                AonAssert.assertTrue(dbClientStatus.contains(operationsStatus), 
                    "Validating Operation Status in the 'Client Details' Table");
                
                Logger.info("Operation Status: {} found for clientId: {} in 'Client Details' table", 
                    dbClientStatus, clientId);
            }
        } while (resultSet.next());
        
        if (!found) {
            throw new SQLException("No matching client record found for clientId: " + clientId);
        }
        
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Client onboarding validation interrupted", e);
    }
}

private ResultSet waitForClientData(PreparedStatement ps, long maxWaitMs, long retryIntervalMs) 
    throws SQLException, InterruptedException {
    
    long startTime = System.currentTimeMillis();
    
    while (true) {
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs; // Data found
        }
        
        if (System.currentTimeMillis() - startTime > maxWaitMs) {
            throw new SQLException("Timeout: Entry not found in database after waiting for " + maxWaitMs + "ms");
        }
        
        Thread.sleep(retryIntervalMs);
    }
}