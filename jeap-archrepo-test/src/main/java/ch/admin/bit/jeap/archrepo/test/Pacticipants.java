package ch.admin.bit.jeap.archrepo.test;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Pacticipants {

    public static final String ARCHREPO_PROPERTY_NAME = "archrepo-service";
    public static final String ARCHREPO = "${" + ARCHREPO_PROPERTY_NAME + "}";

    public static final String DEPLOYMENTLOG_PROPERTY_NAME = "archrepo-deploymentlog-provider";
    public static final String DEPLOYMENTLOG = "${" + DEPLOYMENTLOG_PROPERTY_NAME + "}";

    public static final String MCS_PROPERTY_NAME = "archrepo-messagecontract-provider";
    public static final String MCS = "${" + MCS_PROPERTY_NAME + "}";

    public static final String REACTION_OBSERVER_SERVICE_PROPERTY_NAME = "archrepo-reactionobserverservice-provider";
    public static final String REACTION_OBSERVER_SERVICE = "${" + REACTION_OBSERVER_SERVICE_PROPERTY_NAME + "}";

    public static void setArchrepoPacticipant(String archrepoPacticipantName) {
        System.setProperty(ARCHREPO_PROPERTY_NAME, archrepoPacticipantName);
    }

    public static void setDeploymentlogPacticipant(String deploymentlogPacticipantName) {
        System.setProperty(DEPLOYMENTLOG_PROPERTY_NAME, deploymentlogPacticipantName);
    }

    public static void setMessageContractServicePacticipant(String mcsPacticipantName) {
        System.setProperty(MCS_PROPERTY_NAME, mcsPacticipantName);
    }

    public static void setReactionObserverServicePacticipant(String reactionObserverServicePacticipantName) {
        System.setProperty(REACTION_OBSERVER_SERVICE_PROPERTY_NAME, reactionObserverServicePacticipantName);
    }

}
