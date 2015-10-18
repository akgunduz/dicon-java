package bankor;

/**
 * Created by akgunduz on 10/9/15.
 */
public abstract class Component implements SchedulerCallback, ComponentCallback {

    protected Connector[] connectors = new Connector[HostTypes.getMax()];
    private String rootPath;

    public Component(Unit host, long index, String rootPath) {

        InterfaceCallback interfaceCallback = new InterfaceCallback(this, this);

        int indexDist = (int)(index & 0xFFFF);
        int indexCollector = (int)((index >> 16) & 0xFFFF);
        int indexNode = (int)((index >> 32) & 0xFFFF);

        if (indexDist != 0xFFFF) {

            connectors[HostTypes.HOST_DISTRIBUTOR.getId()] = new Connector(host, indexDist, interfaceCallback, rootPath);

        }

        if (indexCollector != 0xFFFF) {

            if (indexCollector != indexDist) {

                connectors[HostTypes.HOST_COLLECTOR.getId()] = new Connector(host, indexCollector, interfaceCallback, rootPath);

            } else {

                connectors[HostTypes.HOST_COLLECTOR.getId()] = connectors[HostTypes.HOST_DISTRIBUTOR.getId()];
            }
        }

        if (indexNode != 0xFFFF) {

            if (indexNode != indexDist && indexNode != indexCollector) {

                connectors[HostTypes.HOST_NODE.getId()] = new Connector(host, indexNode, interfaceCallback, rootPath);

            } else {

                connectors[HostTypes.HOST_NODE.getId()] = indexDist != 0xFFFF
                        ? connectors[HostTypes.HOST_DISTRIBUTOR.getId()]
                        : connectors[HostTypes.HOST_COLLECTOR.getId()];
            }

        }

        this.rootPath = rootPath;
    }

    public static long generateIndex(int indexDist, int indexCollector, int indexNode) {

        return ((long)indexDist) | ((long)indexCollector << 16) | ((long)indexNode << 32) ;
    }

    public Interfaces getInterfaceType(HostTypes host) {

        if (connectors[host.getId()] != null) {
            return connectors[host.getId()].getInterfaceType();
        }

        return null;
    }

    public long getAddress(HostTypes host) {

        if (connectors[host.getId()] != null) {
            return connectors[host.getId()].getAddress();
        }

        return 0;
    }

    @Override
    public boolean onProcess(long address, Message msg) {

        if (!connectors[msg.getOwner().getType().getId()].getInterfaceType().equals(Address.getInterface(address))) {

         //   LOG_W("Wrong message received : %d from %s, disgarding", msg->getType(), Address::getString(address).c_str());
            return false;
        }

        switch(msg.getOwner().getType()) {

            case HOST_DISTRIBUTOR:
                return processDistributorMsg(address, msg);

            case HOST_NODE:
                return processNodeMsg(address, msg);

            case HOST_COLLECTOR:
                return processCollectorMsg(address, msg);

            default:
                //LOG_W("Wrong message received : %d from %s, disgarding", msg->getType(), Address::getString(address).c_str());
                return false;
        }
    }

    public String getRootPath() {
        return rootPath;
    }

    public void end() {

        if (connectors[HostTypes.HOST_DISTRIBUTOR.getId()] != null) {
            connectors[HostTypes.HOST_DISTRIBUTOR.getId()].end();
        }

        if (connectors[HostTypes.HOST_COLLECTOR.getId()] != null) {
            connectors[HostTypes.HOST_COLLECTOR.getId()].end();
        }

        if (connectors[HostTypes.HOST_NODE.getId()] != null) {
            connectors[HostTypes.HOST_NODE.getId()].end();
        }

    }
}
