package org.wso2.apim.sample.app.sub.migrator;

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

public class DAO {
    private APIMOracleDataSource dataSource;

    public DAO(String dataSourcePath) {
        dataSource = new APIMOracleDataSource(dataSourcePath);
    }

    public Application getApplication(String applicationOwner, String applicationName) throws DAOException {
        try (Connection connection = dataSource.getConnection()){
            return getApplication(applicationOwner, applicationName, connection);
        } catch (SQLException e) {
            throw new DAOException("Error while getting application " + applicationOwner + ":" + applicationName);
        }
    }

    public Application getApplication(String applicationOwner, String applicationName, Connection conn)
            throws DAOException {
        final String getApplicationSQL = "SELECT APPLICATION_ID, NAME, USER_ID, APPLICATION_TIER, DESCRIPTION, "
                + "CALLBACK_URL, GROUP_ID, APPLICATION_STATUS, "
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
            throw new DAOException("Error while getting application " + applicationOwner + ":" + applicationName);
        }
        return application;
    }

    public void addApplication(Application application) throws DAOException {
        final String addAppQuery = "INSERT INTO AM_APPLICATION (NAME, SUBSCRIBER_ID, APPLICATION_TIER, "
                + "CALLBACK_URL, DESCRIPTION, APPLICATION_STATUS, GROUP_ID, CREATED_BY, CREATED_TIME, UUID) "
                + "VALUES (?,?,?, ?,?,?, ?,?,?, ?)";

        Application existingApp = getApplication(application.getOwnerName(), application.getName());
        if (existingApp != null) {
            throw new DAOException(
                    "Application " + application.getOwnerName() + ":" + application.getName() + " already exists.");
        }
        
        Integer subscriberId = getSubscriberId(application.getOwnerName());
        if (subscriberId == null) {
            throw new DAOException("Owner Name: " + application.getOwnerName() + " not found.");
        }

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(addAppQuery)) {
            ps.setString(1, application.getName());
            ps.setInt(2, subscriberId);
            ps.setString(3, application.getApplicationTier());
            ps.setString(4, application.getCallbackUrl());
            ps.setString(5, application.getDescription());
            ps.setString(6, Constants.APPLICATION_APPROVED);
            ps.setString(7, application.getGroupId());
            ps.setString(8, application.getOwnerName());
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            ps.setString(10, UUID.randomUUID().toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DAOException(
                    "Error while adding application " + application.getOwnerName() + ":" + application.getName(), ex);
        }
    }

    public Integer getSubscriberId(String subscriberName) throws DAOException {
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

    public Subscription getSubscription(String apiName, String apiVersion, String applicationName,
            String applicationOwner) throws DAOException {
        Subscription subscription;
        final String getSubscriptionSQL = "SELECT SUBSCRIPTION_ID, TIER_ID, API_ID, APPLICATION_ID, SUB_STATUS, " 
                + "SUBS_CREATE_STATE " 
                + "FROM AM_SUBSCRIPTION WHERE API_ID = ? AND APPLICATION_ID = ?";
        try (Connection conn = dataSource.getConnection()) {
            APIIdentifier apiIdentifier = getAPIIdentifier(apiName, apiVersion, conn);
            if (apiIdentifier == null) {
                throw new DAOException("API " + apiName + ":" + apiVersion + " not found.");
            }
            
            Application application = getApplication(applicationOwner, applicationName);
            if (application == null) {
                throw new DAOException("Application " + applicationOwner + ":" + applicationName + " not found.");
            }

            try (PreparedStatement prepStmt = conn.prepareStatement(getSubscriptionSQL)){
                prepStmt.setInt(1, apiIdentifier.getId());
                prepStmt.setInt(2, application.getId());
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

    public void addSubscription(Subscription subscription) throws DAOException {
        final String addSubscriptionSQL = " INSERT INTO " 
                + "AM_SUBSCRIPTION (TIER_ID,API_ID,APPLICATION_ID,SUB_STATUS,SUBS_CREATE_STATE,CREATED_BY, " 
                + "CREATED_TIME, UUID) VALUES (?,?,?,?,?,?,?,?)";
        String apiName = subscription.getApiIdentifier().getApiName();
        String version = subscription.getApiIdentifier().getVersion();
        String applicationName = subscription.getApplication().getName();
        String applicationOwner = subscription.getApplication().getOwnerName();
        Subscription existingSubscription = getSubscription(apiName,version,applicationName, applicationOwner);

        if (existingSubscription != null) {
            throw new DAOException(
                    "Subscription " + apiName + ":" + version + "::" + applicationOwner + ":" + applicationName
                            + " already exists.");
        }

        try (Connection conn = dataSource.getConnection()) {
            APIIdentifier apiIdentifier = getAPIIdentifier(apiName, version, conn);
            Application application = getApplication(applicationOwner, applicationName, conn);
            try (PreparedStatement ps = conn.prepareStatement(addSubscriptionSQL)){
                ps.setString(1, subscription.getTierId());
                ps.setInt(2, apiIdentifier.getId());
                ps.setInt(3, application.getId());
                ps.setString(4, subscription.getSubscriptionStatus());
                ps.setString(5, subscription.getSubscriptionCreatedStatus());
                ps.setString(6, subscription);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    

    public APIIdentifier getAPIIdentifier(String apiName, String version, Connection connection) throws DAOException {
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
            }
        } catch (SQLException e) {
            throw new DAOException("Error while creating application from result set", e);
        }
        return application;
    }

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
            }
        } catch (SQLException e) {
            throw new DAOException("Error while creating application from result set", e);
        }
        return subscription;
    }
}
