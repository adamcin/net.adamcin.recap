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

package net.adamcin.recap.replication.impl;

import com.day.cq.replication.*;
import net.adamcin.recap.replication.RecapReplicationUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import javax.jcr.Session;
import java.util.Map;

/**
 * Recap content builder implementation to prevent serialization of content, since the recap service syncs content
 * from the real content path. This is one of the benefits provided by using this agent type as an alternative to the
 * default type, as it will not pollute the DataStore with orphaned replication payloads. It comes at the cost of
 * reliably sending content snapshots, as the content that is replicated by this agent is whatever state the content is
 * in when the replication job is processed from the queue, not when it is added to the queue.
 */
@Component
@Service
@Property(name = "name", value = RecapReplicationUtil.SERIALIZATION_TYPE, propertyPrivate = true)
public class RecapContentBuilder implements ContentBuilder {

    public static final String TITLE = "Recap";

    public ReplicationContent create(Session session, ReplicationAction action, ReplicationContentFactory factory)
            throws ReplicationException {
        return ReplicationContent.VOID;
    }

    @Override
    public ReplicationContent create(Session session, ReplicationAction replicationAction, ReplicationContentFactory replicationContentFactory, Map<String, Object> parameters) throws ReplicationException {
        return ReplicationContent.VOID;
    }

    public String getName() {
        return RecapReplicationUtil.SERIALIZATION_TYPE;
    }

    public String getTitle() {
        return TITLE;
    }
}
