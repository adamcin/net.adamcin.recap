package net.adamcin.recap.util;


import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public class OrderableNodesList implements Iterable<String> {
    private Node parent;
    private List<String> nodes;

    public OrderableNodesList(Node parent) throws RepositoryException {
        this.parent = parent;
        this.nodes = new ArrayList<String>();
        buildList();
    }

    private void buildList() throws RepositoryException {
        NodeIterator nodeIterator = parent.getNodes();
        while (nodeIterator.hasNext()) {
            nodes.add(nodeIterator.nextNode().getName());
        }
    }

    public void moveExistingNode(String nodeName, int pos) throws RepositoryException {
        if (!nodes.contains(nodeName)) {
            throw new NoSuchElementException("The node " + nodeName + " was not found under " + parent.getPath());
        }

        nodes.remove(nodeName);
        String orderBeforeNodeName = pos < nodes.size() ? nodes.get(pos) : null;
        parent.orderBefore(nodeName, orderBeforeNodeName);
        nodes.add(pos, nodeName);
    }

    public int getPos(String nodeName) {
        return nodes.indexOf(nodeName);
    }

    public boolean contains(String nodeName) {
        return nodes.contains(nodeName);
    }

    public String getRepositoryPath(String nodeName) throws RepositoryException {
        return nodes.contains(nodeName) ? parent.getNode(nodeName).getPath() : "";
    }

    public Iterator<String> iterator() {
        return nodes.iterator();
    }
}
