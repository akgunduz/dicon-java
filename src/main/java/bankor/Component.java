package bankor;

/**
 * Created by akgunduz on 10/9/15.
 */
public abstract class Component implements SchedulerCallback {

    protected Connector[] connectors = new Connector[2];
    private String rootPath;

    public Component(int interfaceIndexFirst, int interfaceIndexSecond, String rootPath) {

        this.rootPath = rootPath;

        InterfaceCallback interfaceCallback = new InterfaceCallback(this, this);

        connectors[0] = new Connector(interfaceIndexFirst, interfaceCallback, rootPath);

        if (interfaceIndexFirst != interfaceIndexSecond) {

            connectors[1] = new Connector(interfaceIndexSecond, interfaceCallback, rootPath);

        } else {

            connectors[1] = connectors[0];
        }
    }

    public String getRootPath() {
        return rootPath;
    }

    protected void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
}
