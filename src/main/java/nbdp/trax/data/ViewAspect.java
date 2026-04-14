package nbdp.trax.data;

/**
 * Convenience class for use in persisting and loading aspects of views allowing
 * views to preserve some state between instantiations of the view.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class ViewAspect
{
    private String viewId;
    private String aspect;
    
    public String getViewId()
    {
        return viewId;
    }
    public void setViewId(String viewId)
    {
        this.viewId = viewId;
    }
    public String getAspect()
    {
        return aspect;
    }
    public void setAspect(String aspect)
    {
        this.aspect = aspect;
    }
}
