package org.wso2.apim.sample.app.sub.migrator;

import org.wso2.apim.sample.app.sub.migrator.dto.Application;
import org.wso2.apim.sample.app.sub.migrator.dto.Subscription;
import org.wso2.apim.sample.app.sub.migrator.exception.DAOException;

public class Client {
    public static void main(String[] args) throws DAOException {
        String applicationName = "malinthaApp1";
        String applicationOwner = "malintha";
        
        DAO sourceDAO = new DAO("/home/malintha/wso2apim/cur/SCHNEIDERNASUB-5-mig/datasources/source-env.properties");
        DAO targetDAO = new DAO("/home/malintha/wso2apim/cur/SCHNEIDERNASUB-5-mig/datasources/target-env.properties");
        
//        Application application = sourceDAO.getApplication(applicationOwner, applicationName);
//        targetDAO.addApplication(application);

        String apiName = "PizzaShackAPI";
        String version = "1.0.0";

        Subscription subscription = sourceDAO.getSubscription(apiName, version, applicationName, applicationOwner);
        System.out.println(subscription);
    }

}



