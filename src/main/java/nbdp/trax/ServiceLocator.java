package nbdp.trax;

import nbdp.trax.data.I_TraxDao;

/**
 * A class to facilitate injection of dependancies.
 * 
 * @author mboyd
 */
public class ServiceLocator
{
    private static I_TraxDao dao = null;
    private static ServiceLocator instance = new ServiceLocator();

    public I_TraxDao getDAO()
    {
        return ServiceLocator.dao;
    }

    public void setDAO(I_TraxDao dao)
    {
        ServiceLocator.dao = dao;
    }

    /**
     * A mechanism to obtain an instance of the locator without instantiating
     * one. All instances return the same underlying objects so regardless of
     * how one is obtained the same objects will be returned from the locator's
     * accessors.
     */
    public static ServiceLocator getInstance()
    {
        return instance;
    }
}
