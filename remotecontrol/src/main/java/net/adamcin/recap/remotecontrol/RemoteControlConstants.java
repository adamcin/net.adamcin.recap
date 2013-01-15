package net.adamcin.recap.remotecontrol;

/**
 * @author madamcin
 * @version $Id: RemoteControlConstants.java$
 */
public class RemoteControlConstants {
    public static final String SERVLET_LIST_PATH = "/bin/recap/list";
    public static final String SERVLET_STRATEGIES_PATH = "/bin/recap/strategies";
    public static final String KEY_STRATEGY_TYPE = "type";
    public static final String KEY_STRATEGY_LABEL = "label";
    public static final String KEY_STRATEGY_DESCRIPTION = "description";
    public static final String DIRECT_STRATEGY = "direct";
    // ------------------------------------------------
    // Request Parameters related to RecapRequests
    // ------------------------------------------------
    public static final String RP_STRATEGY = ":strategy";
    public static final String RP_SELECTOR_0 = ":selector0";
    public static final String RP_SELECTOR_1 = ":selector1";
    public static final String RP_SELECTOR_2 = ":selector2";
    public static final String RP_SELECTOR_3 = ":selector3";
    public static final String RP_SELECTORS = ":selectors";
    public static final String RP_SUFFIX = ":suffix";
}
