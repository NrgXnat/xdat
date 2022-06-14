package org.nrg.xdat.display.transport.services.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.display.ArcDefinition;
import org.nrg.xdat.display.transport.dao.ArcDefinitionDAO;
import org.nrg.xdat.display.transport.entities.DisplayArcDefFilters;
import org.nrg.xdat.display.transport.entities.DisplayArcDefinition;
import org.nrg.xdat.display.transport.services.ArcDefService;
import org.python.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author Tim Olsen
 *
 * Service implementation for ArcDefs stored via Hibernate.
 */

@Service
public class HibernateArcDefService extends AbstractHibernateEntityService<DisplayArcDefinition, ArcDefinitionDAO> implements ArcDefService {
    @Autowired
    public HibernateArcDefService(final ArcDefinitionDAO dao) {
        _dao = dao;
    }


    /**
     * Finds the SQLObject by name
     *
     * @param name The name for the configuration
     *
     * @return
     */
    @Override
    @Transactional
    public ArcDefinition findArcDefByName(final String name) {
        final DisplayArcDefinition arcDef= getDao().findByName(name,false);
        return wrapArcDefObject(arcDef);
    }

    private ArcDefinition wrapArcDefObject(final DisplayArcDefinition storedAD) {
        final ArcDefinition arcDefine = new ArcDefinition();
        arcDefine.setName(storedAD.getName());
        arcDefine.setBridgeElement(storedAD.getBridgeElement());
        arcDefine.setBridgeField(storedAD.getBridgeField());

        for(final Map.Entry<String,String> entry: storedAD.getCommonFields().entrySet()){
            arcDefine.addCommonField(entry.getKey(), entry.getValue());
        }

        for(final DisplayArcDefFilters arcDefFilters : storedAD.getFilters()){
            arcDefine.addFilter(arcDefFilters.getFilterKey(),arcDefFilters.getFilterType());
        }
        return arcDefine;
    }

    @Override
    @Transactional
    public List<ArcDefinition> findAllArcDefs() {
        final List<ArcDefinition> _return = Lists.newArrayList();

        for(final DisplayArcDefinition arcDef: getDao().findAllEnabled()){
            _return.add(wrapArcDefObject(arcDef));
        }
        return _return;
    }

    /**
     * Finds and deactivate the view with the given name
     *
     * @param name The name of the view to deactivate
     */
    @Override
    @Transactional
    public void deleteByName(String name) {
        final DisplayArcDefinition view = this.getDao().findByName(name,true);
        if (view != null) {
            this.getDao().delete(view);
        }
    }

    @Override
    @Transactional
    public DisplayArcDefinition createArcDef(final ArcDefinition tempAD){
        if(this.getDao().findByName(tempAD.getName(),true)!=null){
            return getDao().findByName(tempAD.getName(),false);
        }

        final DisplayArcDefinition arcDefine = new DisplayArcDefinition();
        arcDefine.setName(tempAD.getName());
        arcDefine.setBridgeElement(tempAD.getBridgeElement());
        arcDefine.setBridgeField(tempAD.getBridgeField());

        for(final Map.Entry<String,String> entry: tempAD.getCommonFields().entrySet()){
            if(StringUtils.isNotBlank(entry.getKey()) &&  StringUtils.isNotBlank(entry.getValue())){
                arcDefine.addCommonField(entry.getKey(), entry.getValue());
            }
        }

        final List<DisplayArcDefFilters> filters = Lists.newArrayList();
        for(final String[] adFilter : tempAD.getFilters()){
            DisplayArcDefFilters filt = new DisplayArcDefFilters();
            filt.setFilterKey(adFilter[0]);
            filt.setFilterType(adFilter[1]);

            filters.add(filt);
        }

        arcDefine.setFilters(filters);

        this.getDao().create(arcDefine);

        return getDao().findByName(arcDefine.getName(),false);
    }

    protected ArcDefinitionDAO getDao() {
        return _dao;
    }

    private static final Log _log = LogFactory.getLog(HibernateArcDefService.class);

    private final ArcDefinitionDAO _dao;
}
