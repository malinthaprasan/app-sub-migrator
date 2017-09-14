package org.wso2.apim.sample.app.sub.migrator.dao;

import org.wso2.apim.sample.app.sub.migrator.dto.APIIdentifier;
import org.wso2.apim.sample.app.sub.migrator.dto.Application;
import org.wso2.apim.sample.app.sub.migrator.dto.Subscription;
import org.wso2.apim.sample.app.sub.migrator.exception.DAOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Database Access Object used for the migration client
 */
public class DAO {
    private APIMOracleDataSource dataSource;

    public DAO(String dataSourcePath) {
        //initializes a hickariCP[https://github.com/brettwooldridge/HikariCP] datasource 
        // from the provided data source properties file
        dataSource = new APIMOracleDataSource(dataSourcePath);
    }

    /**
     * Retrieves an application object from the DB corresponding to the given application owner and application name
     * 
     * @param applicationOwner application owner
     * @param applicationName application name
     * @return Application object
     * @throws DAOException if error occurred during accessing the DB
     */
    public Application getApplication(String applicationOwner, String applicationName) throws DAOException {
        try (Connection connection = dataSource.getConnection()){
            return getApplication(applicationOwner, applicationName, connection);
        } catch (SQLException e) {
            throw new DAOException("Error while getting application " + applicationOwner + ":" + applicationName);
        }
    }

    /**
     * Retrieves an application object from the DB corresponding to the given application owner and application name 
     * using an existing DB connection
     * 
     * @param applicationOwner application owner
     * @param applicationName application name
     * @param conn DB connection
     * @return Application object
     * @throws DAOException if error occurred during accessing the DB
     */
    private Application getApplication(String applicationOwner, String applicationName, Connection conn)
            throws DAOException {
        //To retrieve application details (except keys), we need to merge to tables, which are AM_APPLICATION and AM_SUBSCRIBER.
        // AM_APPLICATION has a foriegn key (user ID) to the AM_SUBSCRIBER and that is where application owner resides
        // so we need to merge the tables to engage the applicationOwner parameter.
        final String getApplicationSQL = "SELECT APPLICATION_ID, NAME, USER_ID, APPLICATION_TIER, DESCRIPTION, "
                + "CALLBACK_URL, GROUP_ID, APPLICATION_STATUS, AM_APPLICATION.CREATED_BY AS CREATED_BY "
                + "FROM AM_APPLICATION, AM_SUBSCRIBER "
                + "WHERE AM_APPLICATION.SUBSCRIBER_ID = AM_SUBSCRIBER.SUBSCRIBER_ID "
                + "AND AM_SUBSCRIBER.USER_ID = ? "
                + "AND AM_APPLICATION.NAME = ?";
        Application application;
        try (PreparedStatement ps = conn.prepareStatement(getApplicationSQL)) {
            ps.setString(1, applicationOwner);
            ps.setString(2, applicationName);
            try (ResultSet rs = ps.executeQuery()) {
                application = createApplicationDTO(rs);
            }
        } catch (SQLException e) {
            throw new DAOException("Error while getting application " + applicationOwner + ":" + applicationName, e);
        }
        return application;
    }

    /**
     * Adds an Application object to the DB
     * 
     * @param application Application object
     * @throws DAOException if error occurred during accessing the DB
     */
    public void addApplication(Application application) throws DAOException {
        //To add an application (except key information), we can just add the Application information to the
        // AM_APPLICATION table by correctly using foreign key from the SubscriberID field to the AM_SUBSCRIBER table
        // to reflect the correct owner of the application 
        final String addAppQuery = "INSERT INTO AM_APPLICATION (NAME, SUBSCRIBER_ID, APPLICATION_TIER, "
                + "CALLBACK_URL, DESCRIPTION, APPLICATION_STATUS, GROUP_ID, CREATED_BY, CREATED_TIME, UUID) "
                + "VALUES (?,?,?, ?,?,?, ?,?,?, ?)";

        //Verifies there's no existing application with same details
        Application existingApp = getApplication(application.getOwnerName(), application.getName());
        if (existingApp != null) {
            throw new DAOException(
                    "Application " + application.getOwnerName() + ":" + application.getName() + " already exists.");
        }

        //Retrieves the subscriber ID (auto increment field) corresponding to the owner of the application
        Integer subscriberId = getSubscriberId(application.getOwnerName());
        if (subscriberId == null) {
            throw new DAOException("Owner Name: " + application.getOwnerName() + " not found.");
        }

        //Adding the application
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(addAppQuery)) {
            ps.setString(1, application.getName());
            ps.setInt(2, subscriberId);
            ps.setString(3, application.getApplicationTier());
            ps.setString(4, application.getCallbackUrl());
            ps.setString(5, application.getDescription());
            ps.setString(6, application.getApplicationStatus());
            ps.setString(7, application.getGroupId());
            ps.setString(8, application.getCreatedBy());
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            ps.setString(10, UUID.randomUUID().toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DAOException(
                    "Error while adding application " + application.getOwnerName() + ":" + application.getName(), ex);
        }
    }

    /**
     * Retrieves the subscriber ID corresponding to the subscriber name
     * 
     * @param subscriberName Subscriber name
     * @return Subscriber ID corresponding to the subscriber name
     * @throws DAOException if error occurred during accessing the DB
     */
    private Integer getSubscriberId(String subscriberName) throws DAOException {
        Integer subscriberId = null;
        final String getSubscriberIdSQL = "SELECT SUBSCRIBER_ID "
                + "FROM AM_SUBSCRIBER "
                + "WHERE USER_ID = ? ";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(getSubscriberIdSQL)) {
            ps.setString(1, subscriberName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    subscriberId = rs.getInt("SUBSCRIBER_ID");
                }
            }

        } catch (SQLException e) {
            throw new DAOException("Error while getting subscriber Id for " + subscriberName);
        }
        return subscriberId;
    }

    /**
     * Retrieves a subscription from DB given the name of the API, application, api version and app owner
     * 
     * @param apiName Name of the API
     * @param apiVersion API version
     * @param applicationName application name
     * @param applicationOwner application owner
     * @return Corresponding subscription from DB
     * @throws DAOException if error occurred during accessing the DB
     */
    public Subscription getSubscription(String apiName, String apiVersion, String applicationName,
            String applicationOwner) throws DAOException {
        Subscription subscription;
        //Retrieves subscription's basic info (corresponding app and API IDs, status etc) from the AM_SUBSCRIPTION table
        final String getSubscriptionSQL = "SELECT SUBSCRIPTION_ID, TIER_ID, API_ID, APPLICATION_ID, SUB_STATUS, " 
                + "SUBS_CREATE_STATE, CREATED_BY " 
                + "FROM AM_SUBSCRIPTION WHERE API_ID = ? AND APPLICATION_ID = ?";
        try (Connection conn = dataSource.getConnection()) {
            //Retrieves the ID of the API from the given name and version
            APIIdentifier apiIdentifier = getAPIIdentifier(apiName, apiVersion, conn);
            if (apiIdentifier == null) {
                throw new DAOException("API " + apiName + ":" + apiVersion + " not found.");
            }
            
            //Retrieves the application details for the given owner and app name
            Application application = getApplication(applicationOwner, applicationName);
            if (application == null) {
                throw new DAOException("Application " + applicationOwner + ":" + applicationName + " not found.");
            }

            try (PreparedStatement prepStmt = conn.prepareStatement(getSubscriptionSQL)){
                prepStmt.setInt(1, apiIdentifier.getId());
                prepStmt.setInt(2, application.getId());
                //Populates the subscription object
                try (ResultSet rs = prepStmt.executeQuery()){
                    subscription = createSubscriptionDTO(rs, application, apiIdentifier);
                }
            }
            
        } catch (SQLException e) {
            throw new DAOException(
                    "Error while getting subscription for API " + apiName + ":" + apiVersion + " and Application "
                            + applicationOwner + ":" + applicationName);
        }
        return subscription;
    }

    /**
     * Adds a subscription to the DB
     * 
     * @param subscription Subscription object
     * @throws DAOException if error occurred during accessing the DB
     */
    public void addSubscription(Subscription subscription) throws DAOException {
        final String addSubscriptionSQL = " INSERT INTO " 
                + "AM_SUBSCRIPTION (TIER_ID,API_ID,APPLICATION_ID,SUB_STATUS,SUBS_CREATE_STATE,CREATED_BY, " 
                + "CREATED_TIME, UUID) VALUES (?,?,?,?,?,?,?,?)";
        String apiName = subscription.getApiIdentifier().getApiName();
        String version = subscription.getApiIdentifier().getVersion();
        String applicationName = subscription.getApplication().getName();
        String applicationOwner = subscription.getApplication().getOwnerName();
        
        //Verifies there is no existing subscription
        Subscription existingSubscription = getSubscription(apiName, version, applicationName, applicationOwner);
        if (existingSubscription != null) {
            throw new DAOException(
                    "Subscription " + apiName + ":" + version + "::" + applicationOwner + ":" + applicationName
                            + " already exists.");
        }

        try (Connection conn = dataSource.getConnection()) {
            //getAPIIdentifier and getApplication methods are used to retrieve the IDs for the application and API
            // included in the subscription object.
            APIIdentifier apiIdentifier = getAPIIdentifier(apiName, version, conn);
            Application application = getApplication(applicationOwner, applicationName, conn);
            try (PreparedStatement ps = conn.prepareStatement(addSubscriptionSQL)){
                ps.setString(1, subscription.getTierId());
                ps.setInt(2, apiIdentifier.getId());
                ps.setInt(3, application.getId());
                ps.setString(4, subscription.getSubscriptionStatus());
                ps.setString(5, subscription.getSubscriptionCreatedStatus());
                ps.setString(6, subscription.getCreatedBy());
                ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                ps.setString(8, UUID.randomUUID().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException(
                    "Error while adding subscription " + apiName + ":" + version + "::" + applicationOwner + ":"
                            + applicationName, e);
        }
    }

    /**
     * Retrieves the ID for a given API name and version using an existing DB connection
     * 
     * @param apiName Name of the API
     * @param version API version
     * @param connection DB connection
     * @return The ID for a given API name and version 
     * @throws DAOException if error occurred during accessing the DB
     */
    private APIIdentifier getAPIIdentifier(String apiName, String version, Connection connection) throws DAOException {
        APIIdentifier apiIdentifier = null;
        String getAPIQuery = "SELECT API_ID, API_NAME, API_VERSION " 
                + "FROM AM_API " 
                + "WHERE API_NAME = ? AND API_VERSION = ?";;

        try (PreparedStatement prepStmt = connection.prepareStatement(getAPIQuery);){
            prepStmt.setString(1, apiName);
            prepStmt.setString(2, version);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    apiIdentifier = new APIIdentifier();
                    apiIdentifier.setApiName(rs.getString("API_NAME"));
                    apiIdentifier.setVersion(rs.getString("API_VERSION"));
                    apiIdentifier.setId(rs.getInt("API_ID"));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error while locating API: " + apiName + ":" + version + " from the database", e);
        }
        return apiIdentifier;
    }

    /**
     * Populates an Application object from the given DB result set
     * 
     * @param rs Result set object
     * @return an Application object from the given DB result set
     * @throws DAOException if error occurred during creating the application object
     */
    private static Application createApplicationDTO(ResultSet rs) throws DAOException {
        Application application = null;
        try {
            if (rs.next()) {
                application = new Application();
                application.setId(rs.getInt("APPLICATION_ID"));
                application.setName(rs.getString("NAME"));
                application.setApplicationTier(rs.getString("APPLICATION_TIER"));
                application.setCallbackUrl(rs.getString("CALLBACK_URL"));
                application.setDescription(rs.getString("DESCRIPTION"));
                application.setGroupId(rs.getString("GROUP_ID"));
                application.setOwnerName(rs.getString("USER_ID"));
                application.setApplicationStatus(rs.getString("APPLICATION_STATUS"));
                application.setCreatedBy(rs.getString("CREATED_BY"));
            }
        } catch (SQLException e) {
            throw new DAOException("Error while creating application from result set", e);
        }
        return application;
    }

    /**
     * Populates a subscription object for the given result set from DB, application and API information
     * 
     * @param rs Result set object
     * @param application Application object
     * @param apiIdentifier API basic info
     * @return A Subscription object for the given result set from DB, application and API information
     * @throws DAOException if error occurred during creating the subscription object
     */
    private static Subscription createSubscriptionDTO(ResultSet rs, Application application,
            APIIdentifier apiIdentifier) throws DAOException {
        Subscription subscription = null;
        try {
            if (rs.next()) {
                subscription = new Subscription();
                subscription.setApiIdentifier(apiIdentifier);
                subscription.setApplication(application);
                subscription.setId(rs.getInt("SUBSCRIPTION_ID"));
                subscription.setTierId(rs.getString("TIER_ID"));
                subscription.setSubscriptionCreatedStatus(rs.getString("SUBS_CREATE_STATE"));
                subscription.setSubscriptionStatus(rs.getString("SUB_STATUS"));
                subscription.setCreatedBy(rs.getString("CREATED_BY"));
            }
        } catch (SQLException e) {
            throw new DAOException("Error while creating application from result set", e);
        }
        return subscription;
    }
}
