/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

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
