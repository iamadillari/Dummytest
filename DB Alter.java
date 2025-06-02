public void alterClientDetailsTableColumn(String alterCmd) throws SQLException {
    String sql = "ROLLBACK_CLNT_COLUMN";
    boolean altered = false;

    try (Connection conn = PdmDbsManager.getInstance().getDataSource(rdrConfig).getConnection();
         Statement stmt = conn.createStatement()) {

        if (alterCmd.equalsIgnoreCase("ALTER")) {
            sql = "ALTER_CLNT_COLUMN";
            stmt.executeUpdate(sql);
            altered = true;
        }

        // Additional test logic can go here...

    } catch (Exception e) {
        System.err.println("Error during alter operation: " + e.getMessage());
        // Handle or rethrow if needed
    } finally {
        // Always rollback if alteration was made
        if (altered) {
            try (Connection conn = PdmDbsManager.getInstance().getDataSource(rdrConfig).getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ROLLBACK_CLNT_COLUMN");
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
        }
    }
}