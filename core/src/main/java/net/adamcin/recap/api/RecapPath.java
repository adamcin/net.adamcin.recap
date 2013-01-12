package net.adamcin.recap.api;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class RecapPath implements Iterable<RecapPath.Segment> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapPath.class);

    private final LinkedList<Segment> segments = new LinkedList<Segment>();
    protected static final String PATTERN_ENCODED_PATH = "(/[^{/]+(\\{\\w+:\\w+(,\\w+:\\w+)*\\})?)+";

    public Iterator<Segment> iterator() {
        return segments.iterator();
    }

    public Segment getLeaf() {
        if (segments.size() > 0) {
            return segments.getLast();
        } else {
            return null;
        }
    }

    public Node getParentNode(Session session) throws RepositoryException {
        return getLeaf().getParent().getNode(session);
    }

    public Node getNode(Session session) throws RepositoryException {
        return getLeaf().getNode(session);
    }

    public String getParentPath() {
        return getLeaf().getParent().getJcrPath();
    }

    public String getName() {
        return getLeaf().getNodeName();
    }

    @Override
    public String toString() {
        return getLeaf() == null ? "" : getLeaf().getJcrPath();
    }

    public static RecapPath parse(final String encodedPath) {
        if (encodedPath == null) {
            throw new NullPointerException("encodedPath");
        }

        if (!encodedPath.matches(PATTERN_ENCODED_PATH)) {
            throw new IllegalArgumentException("encodedPath");
        }

        RecapPath recapPath = new RecapPath();

        String[] rawSegments = StringUtils.split(encodedPath, "/");

        Segment parentSegment = null;
        for (String rawSegment : rawSegments) {
            String nodeName = rawSegment;
            String primaryType = null;
            Set<String> mixinTypes = new HashSet<String>();
            int braceIndex = rawSegment.indexOf("{");
            if (braceIndex >= 0) {
                nodeName = rawSegment.substring(0, braceIndex);
                rawSegment = rawSegment.substring(braceIndex + 1, rawSegment.length() - 1);
                if (rawSegment.contains(",")) {
                    String[] types = StringUtils.split(rawSegment, ",");
                    if (types.length > 0) {
                        List<String> typeList = Arrays.asList(types);

                        boolean isFirst = true;
                        for (String type : typeList) {
                            if (isFirst) {
                                primaryType = type;
                            } else {
                                mixinTypes.add(type);
                            }

                            isFirst = false;
                        }
                    }
                } else {
                    primaryType = rawSegment;
                }
            }

            LOGGER.debug("[parse] nodeName={}, primaryType={}, mixinTypes={}", new Object[]{nodeName, primaryType, mixinTypes});
            Segment segment = new Segment(parentSegment, nodeName, primaryType, mixinTypes);
            recapPath.segments.add(segment);
            parentSegment = segment;
        }

        return recapPath;
    }

    public static RecapPath build(final Node node) throws RepositoryException {
        RecapPath path = new RecapPath();
        Stack<Node> nodeStack = new Stack<Node>();

        Node parent = node;
        while (parent.getDepth() > 0) {
            nodeStack.push(parent);
            parent = parent.getParent();
        }

        Segment parentSegment = null;
        while (!nodeStack.empty()) {
            Node current = nodeStack.pop();
            Set<String> mixinTypes = new HashSet<String>();
            for (NodeType mixin : current.getMixinNodeTypes()) {
                mixinTypes.add(mixin.getName());
            }
            Segment segment =
                    new Segment(parentSegment, current.getName(),
                            current.getPrimaryNodeType().getName(), mixinTypes);
            path.segments.add(segment);
            parentSegment = segment;
        }

        return path;
    }

    public static class Segment {
        final Segment parent;
        final String nodeName;
        final String primaryType;
        final Set<String> mixinTypes;

        public Segment(final Segment parent, final String nodeName, final String primaryType, final Set<String> mixinTypes) {
            this.parent = parent;
            this.nodeName = nodeName;
            this.primaryType = primaryType;
            if (mixinTypes != null) {
                this.mixinTypes = Collections.unmodifiableSet(mixinTypes);
            } else {
                this.mixinTypes = Collections.emptySet();
            }
        }

        public Segment getParent() {
            return parent;
        }

        public String getNodeName() {
            return nodeName;
        }

        public String getPrimaryType() {
            return primaryType;
        }

        public Set<String> getMixinTypes() {
            return mixinTypes;
        }

        public Node getNode(Session session) throws RepositoryException {
            return session.getNode(this.getJcrPath());
        }

        public int establishPath(Session session) throws RepositoryException {
            Node parentNode = null;
            int nodesCreatedOrUpdated = 0;
            boolean updatedSelf = false;
            if (this.parent != null) {
                nodesCreatedOrUpdated = this.parent.establishPath(session);
                parentNode = this.parent.getNode(session);
            } else {
                parentNode = session.getRootNode();
            }

            Node me = null;
            if (parentNode.hasNode(this.nodeName)) {
                me = parentNode.getNode(this.nodeName);
                if (this.primaryType != null && !me.isNodeType(this.primaryType)) {
                    me.setPrimaryType(this.primaryType);
                    updatedSelf = true;
                }
            } else if (this.primaryType != null) {
                me = parentNode.addNode(this.nodeName, this.primaryType);
                updatedSelf = true;
            } else {
                me = parentNode.addNode(this.nodeName);
                updatedSelf = true;
            }

            for (String mix : mixinTypes) {
                if (!me.isNodeType(mix)) {
                    me.addMixin(mix);
                    updatedSelf = true;
                }
            }

            if (updatedSelf) {
                nodesCreatedOrUpdated += 1;
            }

            return nodesCreatedOrUpdated;
        }

        public String encodePath() {
            String basePath = "";
            if (this.parent != null) {
                basePath = this.parent.encodePath();
            }

            StringBuilder sb = new StringBuilder(basePath).append("/").append(this.nodeName);
            sb.append("{").append(this.primaryType);
            for (String mix : this.mixinTypes) {
                sb.append(",").append(mix);
            }
            return sb.append("}").toString();
        }

        public String getJcrPath() {
            String basePath = "";
            if (this.parent != null) {
                basePath = this.parent.getJcrPath();
            }

            return basePath + "/" + this.nodeName;
        }
    }
}
