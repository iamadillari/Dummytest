import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ApiDatabaseTestFramework {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";
    private static final String DB_USER = "testuser";
    private static final String DB_PASSWORD = "testpass";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int POLL_INTERVAL_SECONDS = 2;

    /**
     * Method 1: Simple Thread.sleep (Not Recommended for production)
     */
    @Test
    public void testApiWithSimpleWait() {
        // Make API call
        Response response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"name\":\"John\",\"email\":\"john@example.com\"}")
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract().response();

        String userId = response.jsonPath().getString("id");

        try {
            // Simple wait - not ideal but sometimes necessary
            Thread.sleep(5000); // Wait 5 seconds
            
            // Check database
            boolean userExists = checkUserInDatabase(userId);
            Assert.assertTrue(userExists, "User should exist in database");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }

    /**
     * Method 2: Polling with timeout (Recommended)
     */
    @Test
    public void testApiWithPollingWait() {
        // Make API call
        Response response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"name\":\"Jane\",\"email\":\"jane@example.com\"}")
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract().response();

        String userId = response.jsonPath().getString("id");

        // Poll database until user appears or timeout
        boolean userExists = waitForUserInDatabase(userId, DEFAULT_TIMEOUT_SECONDS);
        Assert.assertTrue(userExists, "User should exist in database within timeout period");
    }

    /**
     * Method 3: Using Awaitility library (Most Robust)
     */
    @Test
    public void testApiWithAwaitility() {
        // First add Awaitility dependency to your project:
        // <dependency>
        //     <groupId>org.awaitility</groupId>
        //     <artifactId>awaitility</artifactId>
        //     <version>4.2.0</version>
        //     <scope>test</scope>
        // </dependency>

        // Make API call
        Response response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"name\":\"Bob\",\"email\":\"bob@example.com\"}")
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract().response();

        String userId = response.jsonPath().getString("id");

        // Using Awaitility for robust waiting
        /*
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> checkUserInDatabase(userId));
        */
    }

    /**
     * Method 4: Advanced polling with custom conditions
     */
    @Test
    public void testApiWithAdvancedWait() {
        // Make API call to update user status
        Response response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"status\":\"ACTIVE\"}")
                .when()
                .put("/api/users/123/status")
                .then()
                .statusCode(200)
                .extract().response();

        // Wait for specific database state
        boolean statusUpdated = waitForUserStatus("123", "ACTIVE", 30);
        Assert.assertTrue(statusUpdated, "User status should be updated to ACTIVE");
    }

    /**
     * Utility method to check if user exists in database
     */
    private boolean checkUserInDatabase(String userId) {
        String query = "SELECT COUNT(*) FROM users WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
        
        return false;
    }

    /**
     * Polling method with timeout
     */
    private boolean waitForUserInDatabase(String userId, int timeoutSeconds) {
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(timeoutSeconds);
        
        while (Instant.now().isBefore(endTime)) {
            if (checkUserInDatabase(userId)) {
                System.out.println("User found in database after " + 
                    Duration.between(startTime, Instant.now()).getSeconds() + " seconds");
                return true;
            }
            
            try {
                TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrupted", e);
            }
        }
        
        System.err.println("Timeout waiting for user in database");
        return false;
    }

    /**
     * Wait for specific user status
     */
    private boolean waitForUserStatus(String userId, String expectedStatus, int timeoutSeconds) {
        String query = "SELECT status FROM users WHERE id = ?";
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(timeoutSeconds);
        
        while (Instant.now().isBefore(endTime)) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String currentStatus = rs.getString("status");
                    if (expectedStatus.equals(currentStatus)) {
                        System.out.println("User status updated to " + expectedStatus + " after " + 
                            Duration.between(startTime, Instant.now()).getSeconds() + " seconds");
                        return true;
                    }
                }
                
            } catch (SQLException e) {
                System.err.println("Database error during status check: " + e.getMessage());
            }
            
            try {
                TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Status polling interrupted", e);
            }
        }
        
        return false;
    }

    /**
     * Generic database polling utility
     */
    public boolean waitForDatabaseCondition(DatabaseCondition condition, int timeoutSeconds) {
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(timeoutSeconds);
        
        while (Instant.now().isBefore(endTime)) {
            if (condition.check()) {
                return true;
            }
            
            try {
                TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Database condition polling interrupted", e);
            }
        }
        
        return false;
    }

    /**
     * Functional interface for database conditions
     */
    @FunctionalInterface
    public interface DatabaseCondition {
        boolean check();
    }

    /**
     * Example usage of generic database condition
     */
    @Test
    public void testWithGenericDatabaseCondition() {
        // Make API call
        Response response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"orderId\":\"ORD123\",\"amount\":100.00}")
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201)
                .extract().response();

        // Wait for order to be processed
        boolean orderProcessed = waitForDatabaseCondition(() -> {
            String query = "SELECT status FROM orders WHERE order_id = 'ORD123'";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return "PROCESSED".equals(rs.getString("status"));
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
            return false;
        }, 30);

        Assert.assertTrue(orderProcessed, "Order should be processed within 30 seconds");
    }
}
