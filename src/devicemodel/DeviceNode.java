package devicemodel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;

public class DeviceNode implements PropertyChangeListener {

    public static final String PROPERTY_CHANGE_NAME = "update";
    // listeners will get updates fired when this node's value or children's values change
    // note that the public method setValue() does not fire an event, but allows 
    // access for the user to set the value at initialization, etc
    // usage of the update(DeviceNode) method is preferred as it will fire updates 
    // and handle all update change aggregation and parent recursion
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    protected DeviceNode parent = null;
    protected String name = null;
    protected String value = "";
    
    // stores all of this node's children by name
    protected final Map<String, DeviceNode> children = new ConcurrentHashMap<>();
    // stores references to ALL children and grandchildren of this node
    protected final Map<String, DeviceNode> allChildren = new ConcurrentHashMap<>();
    // assumption is that attributes do not regularly change and do not fire events
    // the element values store the changing data; attributes describe the data
    // value changes cause events to be triggered and fired, but include attribute for the changed elements
    protected final Map<String, String> attributes = new ConcurrentHashMap<>();
    //
    // these are access handlers for the node
    // SET: called at set(DeviceNode) when requesting this node's value to change
    //      by default this does nothing, but add in a handle to handle the logic
    //      for setting some value this device node model represents
    //
    // UPDATE: called at update(DeviceNode) when updating this node's value, children, or attributes
    //      by default, this updates the node's value and attributes accordingly, but
    //      add in a handle to change this behavior (e.g. send data to a log file instead)
    private NodeHandler setHandle;
    private NodeHandler updateHandle;
    private NodeGetHandler getHandle;

    public DeviceNode(String name) {
        this(name, null);
    }

    public DeviceNode(String name, DeviceNode parent) {
        this.name = name;
        if (parent != null) {
            try {
                parent.addChild(this);
            } catch (Exception ex) {
                // we just made this child; it'll always have a null parent
            }
        }
    }

    public String getName() {
        return name;
    }

    public PropertyChangeSupport getChangeSupport() {
        return changeSupport;
    }

    public DeviceNode get() {

        DeviceNode ret = null;
        
        // fire getHandle, if it's attached
        if (this.getHandle != null) {
            ret = getHandle.handle();
        } else {
            // otherwise do a shallow clone on this node
            ret = this.cloneShallow();
        }

        Iterator<String> i = getChildren().keySet().iterator();

        // loop through all children 
        while (i.hasNext()) {
            String s = i.next();
            if (this.getChildren().containsKey(s)) {
                try {
                    ret.addChild(this.getChild(s).get());
                } catch (Exception ex) {
                    Logger.getLogger(DeviceNode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return ret;
    }

    public void set(DeviceNode n) {

        // make sure it's this node
        if (n.getName().equalsIgnoreCase(this.name)) {

            // fire setHandle, if it's attached
            if (this.setHandle != null) {
                setHandle.handle(n);
            }

            Iterator<String> i = n.getChildren().keySet().iterator();

            // loop through all children 
            while (i.hasNext()) {
                String s = i.next();
                if (this.getChildren().containsKey(s)) {
                    this.getChild(s).set(n.getChild(s));
                }
            }
        }
    }

    // update to be called from external classes; this calls the recursive loop
    // to update all the children (if applicable) and fire the aggregated events
    public void update(DeviceNode n) {

        // do recursive update & fire events as needed
        DeviceNode change = updateNode(n);

        // At this point we've updated everything and the end of update() fired
        // a change event for this node (if any); if there was a change, continue
        // up the tree for all the parents
        if (change != null) {

            if (parent != null) {
                parent.childEventFired(change);
            }
        }
    }

    private void childEventFired(DeviceNode n) {

        // this node will be the root node for the event
        DeviceNode change = this.cloneShallow();

        // add child event
        try {
            change.addChild(n);
        } catch (Exception ex) {
            // we just made this child; it'll always have a null parent
        }

        // fire event
        changeSupport.firePropertyChange(PROPERTY_CHANGE_NAME, null, change);

        // notify parent, if applicable
        if (parent != null) {
            parent.childEventFired(change);
        }
    }

    // do not use this one
    private DeviceNode updateNode(DeviceNode n) {
        // keep track if anything changed and should fire event
        DeviceNode changeEvent = null;

        // make sure it's this node
        if (n.getName().equalsIgnoreCase(this.name)) {

            boolean handleHere = true;
            // fire updateHandle, if it's attached
            if (this.updateHandle != null) {
                handleHere = updateHandle.handle(n);
            }
            if (handleHere) {
                // update attributes
                this.getAttributes().putAll(n.getAttributes());

                // set value, if needed
                if (n.getValue() != null) {
                    if (!this.value.equals(n.getValue())) {
                        this.setValue(n.getValue());

                        if (changeEvent == null) {
                            changeEvent = this.cloneShallow();
                        }
                    }
                }
            }

            Iterator<String> i = n.getChildren().keySet().iterator();

            // merge children; update or add
            while (i.hasNext()) {
                String s = i.next();
                boolean added = false;

                // if child does not exist yet, add it
                if (!this.getChildren().containsKey(s)) {
                    try {
                        this.addChild(n.getChild(s).cloneShallow());
                    } catch (Exception ex) {
                        // we just made this child; it'll always have a null parent
                    }
                    added = true;
                }

                if (this.getChildren().containsKey(s)) {
                    // update child
                    DeviceNode childUpdate = this.getChild(s).updateNode(n.getChild(s));

                    // either updated child or added (if added, won't get childUpdate)
                    if (childUpdate != null || added) {
                        if (changeEvent == null) {
                            changeEvent = this.cloneShallow();
                        }
                        // add the child update if there was one
                        if (childUpdate != null) {
                            try {
                                changeEvent.addChild(childUpdate);
                            } catch (Exception ex) {
                                // we just made this child; it'll always have a null parent
                            }
                        } // if the child was added, but no grandchildren changed, still fire event
                        else {
                            try {
                                changeEvent.addChild(n.getChild(s).cloneShallow());
                            } catch (Exception ex) {
                                // we just made this child; it'll always have a null parent
                            }
                        }
                    }
                }
            }
        }

        // fire event for this node if it or any children changed
        if (changeEvent != null) {
            changeSupport.firePropertyChange(PROPERTY_CHANGE_NAME, null, changeEvent);
        }

        return changeEvent;
    }

    // shallow clone, mostly for event generation purposes
    public DeviceNode cloneShallow() {
        DeviceNode n = new DeviceNode(this.getName());
        n.setValue(this.value);
        for (String str : this.getAttributes().keySet()) {
            n.addAttribute(str, this.getAttribute(str));
        }
        return n;
    }

    // using this will NOT fire an event, use update() for that
    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // should only be used internally; add/remove child methods should be used
    protected void setParent(DeviceNode parent) {
        this.parent = parent;
        this.changeSupport.addPropertyChangeListener(parent);
    }

    public void removeChild(String name) {
        if (children.containsKey(name)) {
            removeChild(children.get(name));
        }
    }

    public void removeChild(DeviceNode child) {
        if (children.containsValue(child)) {
            // remove this listener
            child.changeSupport.removePropertyChangeListener(this);
            // remove the child
            synchronized (children) {
                children.remove(child.getName());
            }
            String path = "/" + child.getName();

            // deregister the child tree
            deregisterByPrefix(path);

            // deregister from grandparent trees
            parent.deregisterGrandchild("/" + this.name + path);
        }
    }

    public void addChild(DeviceNode child) throws Exception {
        if (child.getParent() == null) {
            child.setParent(this);
        } else {
            throw new Exception("Child " + child.getName() + " already has parent " + child.getParent().getName());
        }

        synchronized (children) {
            this.children.put(child.getName(), child);
        }

        synchronized (allChildren) {
            // first register this child
            String cPath = "/" + child.getName();
            this.allChildren.put(cPath, child);

            // recurse up adding child (grandchild)
            if (parent != null) {
                parent.registerGrandchild(cPath, this.name, child);
            }

            // add this child's grandchildren
            for (String gcKey : child.getAllChildren().keySet()) {
                String gcPath = cPath + gcKey;
                DeviceNode grandchild = child.getAllChildren().get(gcKey);

                // register here
                this.allChildren.put(gcPath, grandchild);

                // recurse up adding grandchild
                if (parent != null) {
                    parent.registerGrandchild(gcPath, this.name, grandchild);
                }
            }
        }
    }

    protected void deregisterGrandchild(String path) {

        // path will contain /child/grandchild
        if (allChildren.containsKey(path)) {
            deregisterByPrefix(path);

            if (parent != null) {
                path = "/" + this.name + path;
                parent.deregisterGrandchild(path);
            }
        }
    }

    protected void deregisterByPrefix(String prefix) {
        // remove all references
        synchronized (allChildren) {
            Iterator<String> i = allChildren.keySet().iterator();

            while (i.hasNext()) {
                String next = i.next();
                if (next.startsWith(prefix)) {
                    i.remove();
                }
            }
        }
    }

    protected void registerGrandchild(String path, String childName, DeviceNode n) {
        synchronized (allChildren) {
            path = "/" + childName + path;
            allChildren.put(path, n);
        }

        if (parent != null) {
            parent.registerGrandchild(path, this.name, n);
        }
    }

    public DeviceNode getParent() {
        return parent;
    }

    public DeviceNode getChildByPath(String path) {
        if (allChildren.containsKey(path)) {
            return allChildren.get(path);
        } else {
            return null;
        }
    }

    // read-only; adding should go through addChild()
    public Map<String, DeviceNode> getAllChildren() {
        return Collections.unmodifiableMap(allChildren);
    }

    // read-only; adding should go through addChild()
    public Map<String, DeviceNode> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public List<String> getChildrenNamesSorted() {
        LinkedList<String> leaves = new LinkedList();
        LinkedList<String> branches = new LinkedList();
        synchronized (children) {
            for (DeviceNode d : children.values()) {
                if (d.getChildren().isEmpty()) {
                    leaves.add(d.getName());
                } else {
                    branches.add(d.getName());
                }
            }
        }
        Collections.sort(leaves);
        Collections.sort(branches);
        leaves.addAll(branches);
        return leaves;
    }

    public DeviceNode getChild(String name) {
        return children.get(name);
    }

    public DeviceNode addAttribute(String name, String attribute) {
        this.attributes.put(name, attribute);
        return this;
    }

    public synchronized Map<String, String> getAttributes() {
        return attributes;
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public static String trimPath(String str, int levels) {
        StringBuilder sb = new StringBuilder(str);

        while (levels != 0) {
            int end = sb.indexOf("/", 1);
            sb.delete(0, end);
            levels--;
        }
        return sb.toString();
    }

    public String getNodePath() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.parent != null ? this.parent.getNodePath() : "").append("/").append(this.getName());

        return sb.toString();
    }

    public NodeHandler getSetHandle() {
        return setHandle;
    }

    public void setSetHandle(NodeHandler setHandle) {
        this.setHandle = setHandle;
    }

    public NodeHandler getUpdateHandle() {
        return updateHandle;
    }

    public void setUpdateHandle(NodeHandler updateHandle) {
        this.updateHandle = updateHandle;
    }

    public NodeGetHandler getGetHandle() {
        return getHandle;
    }

    public void setGetHandle(NodeGetHandler getHandle) {
        this.getHandle = getHandle;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // we don't do anything here yet...
        // the updateNode() and childEventFired() methods take care of recursion
    }
}
