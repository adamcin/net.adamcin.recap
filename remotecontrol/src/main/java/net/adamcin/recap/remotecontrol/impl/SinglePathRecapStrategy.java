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

package net.adamcin.recap.remotecontrol.impl;

import net.adamcin.recap.remotecontrol.RecapRemoteException;
import net.adamcin.recap.remotecontrol.RecapStrategy;
import org.apache.felix.scr.annotations.Component;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author madamcin
 * @version $Id: SinglePathRecapStrategy.java$
 */
@Component(factory = "net.adamcin.recap.RecapStrategy/single",
        metatype = true,
        label = "Single Path",
        description = "Recursively copy a single path from a remote CRX repository to this one.")
public class SinglePathRecapStrategy implements RecapStrategy {

    public Iterator<Node> listNodes(SlingHttpServletRequest recapRequest)
            throws RecapRemoteException {

        Resource resource = recapRequest.getResourceResolver().
                getResource(recapRequest.getRequestPathInfo().getSuffix());

        if (resource != null) {
            Node resourceNode = resource.adaptTo(Node.class);
            if (resourceNode != null) {
                return Arrays.asList(new Node[]{resourceNode}).iterator();
            }
        }
        return null;
    }
}
