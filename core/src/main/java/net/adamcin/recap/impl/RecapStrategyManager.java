package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapStrategy;
import net.adamcin.recap.api.RecapStrategyDescriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public class RecapStrategyManager {

    private static final Logger log = LoggerFactory.getLogger(RecapStrategyManager.class);

    private final BundleContext bundleContext;
    private final MetaTypeService metaTypeService;

    private final Map<Object, ServiceReference> refMap = new HashMap<Object, ServiceReference>();
    private final Map<Object, ComponentInstance> instanceMap = new HashMap<Object, ComponentInstance>();

    private static final String FACTORY_PROPERTY = "component.factory";
    private static final String PROP_OCD_NAME = "ocd.name";
    private static final String PROP_OCD_DESC = "ocd.description";

    public RecapStrategyManager(BundleContext bundleContext, MetaTypeService metaTypeService) {
        this.bundleContext = bundleContext;
        this.metaTypeService = metaTypeService;
    }

    public RecapStrategy newInstance(String factoryName) {
        return this.newInstance(factoryName, null);
    }

    @SuppressWarnings("unchecked")
    public RecapStrategy newInstance(String factoryName, Dictionary<?,?> properties) {

        if(properties == null){
            properties = new Properties();
        }

        ServiceReference[] refs = this.getServiceReferences(factoryName);

        if ((refs == null) || (refs.length == 0)) {
            log.debug("refs is null or empty");
            return null;
        }

        ServiceReference ref = refs[0];

        if(refs.length > 1){
            for(int i = 1; i < refs.length; i++){
                this.bundleContext.ungetService(refs[i]);
            }
        }

        ComponentFactory factory = (ComponentFactory)this.bundleContext.getService(ref);
        ComponentInstance instance = factory.newInstance(properties);

        Object component = instance.getInstance();

        if (component == null) {
            log.error("Unable to get RecapStrategy instance: " + factoryName);
            instance.dispose();
            this.bundleContext.ungetService(ref);
        } else {
            synchronized (this.refMap) {
                this.refMap.put(component, ref);
                this.instanceMap.put(component, instance);
            }
        }

        return (RecapStrategy) component;
    }

    public void release(RecapStrategy strategy) {

        synchronized (this.refMap) {

            if (this.instanceMap.containsKey(strategy)) {
                this.instanceMap.get(strategy).dispose();
                this.instanceMap.remove(strategy);
            }

            if (this.refMap.containsKey(strategy)) {
                this.bundleContext.ungetService(this.refMap.get(strategy));
                this.refMap.remove(strategy);
            }
        }
    }

    public void releaseAll() {
        synchronized (this.refMap) {
            for (ComponentInstance instance : this.instanceMap.values()) {
                instance.dispose();
            }

            for (ServiceReference ref : this.refMap.values()) {
                this.bundleContext.ungetService(ref);
            }

            this.instanceMap.clear();
            this.refMap.clear();
        }
    }

    public Map<String,Object> getFactoryProperties(String factoryName){
        Map<String,Map<String,Object>> factories = this.getAllFactoryProperties(factoryName);
        return factories.get(factoryName);
    }

    public Map<String,Map<String,Object>> getAllFactoryProperties(String factoryFilter){
        HashMap<String,Map<String,Object>> factories = new HashMap<String,Map<String,Object>>();

        ServiceReference[] refs = this.getServiceReferences(factoryFilter);

        if ((refs == null) || (refs.length == 0)) {
            log.debug("refs is null or empty");
            return factories;
        }

        for(ServiceReference ref : refs){
            Map<String,Object> props = new HashMap<String,Object>();
            for(String key : ref.getPropertyKeys()){
                props.put(key, ref.getProperty(key));
            }
            if (this.metaTypeService != null) {
                MetaTypeInformation bundleInfo = this.metaTypeService.getMetaTypeInformation(ref.getBundle());
                if (bundleInfo != null) {
                    ObjectClassDefinition ocd = bundleInfo.getObjectClassDefinition((String) ref.getProperty(Constants.SERVICE_PID), null);
                    if (ocd != null) {
                        String ocdName = ocd.getName();
                        if (ocdName != null) {
                            props.put(PROP_OCD_NAME, ocdName);
                        }
                        String ocdDesc = ocd.getDescription();
                        if (ocdDesc != null) {
                            props.put(PROP_OCD_DESC, ocdDesc);
                        }
                    }
                }
            }
            String factoryName = ((String)ref.getProperty(FACTORY_PROPERTY)).substring(RecapStrategy.class.getName().length() + 1);
            factories.put(factoryName, props);
            this.bundleContext.ungetService(ref);
        }

        return factories;
    }

    private ServiceReference[] getServiceReferences(String factoryFilter){
        if(factoryFilter == null){
            factoryFilter = "*";
        }

        String serviceReferenceQuery = "(" + FACTORY_PROPERTY + "=" + RecapStrategy.class.getName() + '/' + factoryFilter + ')';
        log.debug(serviceReferenceQuery);

        try {
            return this.bundleContext.getServiceReferences(ComponentFactory.class.getName(), serviceReferenceQuery);
        } catch (InvalidSyntaxException e) {
            log.error("Somehow the query syntax was invalid: " + serviceReferenceQuery, e);
            return null;
        }
    }

    public List<RecapStrategyDescriptor> listStrategyDescriptors() {
        List<RecapStrategyDescriptor> strategies = new ArrayList<RecapStrategyDescriptor>();
        Set<Map.Entry<String, Map<String, Object>>> entries = this.getAllFactoryProperties(null).entrySet();
        for (Map.Entry<String, Map<String, Object>> entry : entries) {
            RecapStrategyDescriptorImpl descriptor = new RecapStrategyDescriptorImpl();
            descriptor.setType(entry.getKey());
            descriptor.setLabel((String) entry.getValue().get(PROP_OCD_NAME));
            descriptor.setDescription((String) entry.getValue().get(PROP_OCD_DESC));
            strategies.add(descriptor);
        }
        return Collections.unmodifiableList(strategies);

    }
}