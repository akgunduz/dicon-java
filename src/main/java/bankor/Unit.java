package bankor;

/**
 * Created by akgunduz on 10/18/15.
 */
public class Unit {

    private int unit;

    public Unit(int unit) {
        this.unit = unit;
    }

    public Unit(HostTypes type, short id) {
        this.unit = type.getId() << 16 | id;
    }

    public Unit(HostTypes type) {
        this.unit = type.getId() << 16 | Util.getID();
    }

    public int getUnit() {
        return unit;
    }

    public HostTypes getType() {
        return HostTypes.getHost(unit >> 16);
    }

    public short getID() {
        return (short) (unit & 0xFFFF);
    }
}
