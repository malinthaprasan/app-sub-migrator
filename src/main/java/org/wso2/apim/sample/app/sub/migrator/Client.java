package org.wso2.apim.sample.app.sub.migrator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apim.sample.app.sub.migrator.dao.DAO;
import org.wso2.apim.sample.app.sub.migrator.dto.Application;
import org.wso2.apim.sample.app.sub.migrator.dto.Subscription;
import org.wso2.apim.sample.app.sub.migrator.exception.DAOException;

public class Client {
    private static final String ARG_MIGRATE_APP = "migrateApp";
    private static final String ARG_MIGRATE_SUB = "migrateSub";
    private static final String SYS_PROP_APP_NAME = "appName";
    private static final String SYS_PROP_APP_OWNER = "appOwner";
    private static final String SYS_PROP_API_NAME = "apiName";
    private static final String SYS_PROP_API_VERSION = "apiVersion";
    private static final String SYS_PROP_SRC_CONF = "srcEnvConfig";
    private static final String SYS_PROP_SRC_CONF_DEFAULT = "datasources/source-env.properties";
    private static final String SYS_PROP_TARGET_CONF = "targetEnvConfig";
    private static final String SYS_PROP_TARGET_CONF_DEFAULT = "datasources/target-env.properties";

    private static Logger log = LoggerFactory.getLogger("org.wso2.apim.sample.app.sub.migrator.Client");

    public static void main(String[] args) throws DAOException {
        //First argument should be migrateApp or migrateSub
        if (args == null || args.length == 0 || !(ARG_MIGRATE_APP.equals(args[0]) || ARG_MIGRATE_SUB.equals(args[0]))) {
            System.out.println("Invalid arguments.");
            printHelp();
            return;
        }

        //Retrieve params via system properties
        String applicationName = System.getProperty(SYS_PROP_APP_NAME);
        String applicationOwner = System.getProperty(SYS_PROP_APP_OWNER);
        String apiName = System.getProperty(SYS_PROP_API_NAME);
        String apiVersion = System.getProperty(SYS_PROP_API_VERSION);

        String srcEnvProperties = System.getProperty(SYS_PROP_SRC_CONF) != null ?
                System.getProperty(SYS_PROP_SRC_CONF) : SYS_PROP_SRC_CONF_DEFAULT;
        String targetEnvProperties = System.getProperty(SYS_PROP_TARGET_CONF) != null ?
                System.getProperty(SYS_PROP_TARGET_CONF) : SYS_PROP_TARGET_CONF_DEFAULT;

        //Create data access objects for both environments
        DAO sourceDAO = new DAO(srcEnvProperties);
        DAO targetDAO = new DAO(targetEnvProperties);

        if (ARG_MIGRATE_APP.equals(args[0])) {
            if (StringUtils.isEmpty(applicationName) || StringUtils.isEmpty(applicationOwner)) {
                log.error("Application name or owner can't be empty.");
                return;
            }
            //retrieves the application
            Application application = sourceDAO.getApplication(applicationOwner, applicationName);
            log.info("Retrieved application data:\n" + application);
            log.info("Migrating the application to target environment..");
            //migrates the application
            targetDAO.addApplication(application);
            log.info("Application migrated successfully.");
        } else if (ARG_MIGRATE_SUB.equals(args[0])) {
            if (StringUtils.isEmpty(applicationName) || StringUtils.isEmpty(applicationOwner) || StringUtils
                    .isEmpty(apiName) || StringUtils.isEmpty(apiVersion)) {
                log.error("Application name/application owner/API name and version can't be empty.");
                return;
            }
            //retrieves the subscription
            Subscription subscription = sourceDAO
                    .getSubscription(apiName, apiVersion, applicationName, applicationOwner);
            log.info("Retrieved subscription data:\n" + subscription);
            log.info("Migrating the subscription to target environment..");
            //migrates the subscription
            targetDAO.addSubscription(subscription);
            log.info("Subscription migrated successfully.");
        }
    }

    private static void printHelp() {
        System.out.println("Usage:\n"
                + "\n"
                + "Migrating an application:\n"
                + "    [..] migrateApp -DappName=application1 -DappOwner=admin [-DsrcEnvConfig=source-env.properties] " 
                + "[-DtargetEnvConfig=target-env.properties]\n"
                + "\n"
                + "Migrating a subscription:\n"
                + "    [..] migrateSub -DappName=application1 -DappOwner=admin -DapiName=PizzaShackAPI " 
                + "-DapiVersion=1.0.0 [-DsrcEnvConfig=source-env.properties] " 
                + "[-DtargetEnvConfig=target-env.properties]\n");
        
    }
}



