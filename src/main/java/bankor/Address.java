package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public abstract class Address {

    long address;

    Address() {

    }

    Address(long address) {
        this.address = address;
    }

    abstract Interfaces getInterface();
    abstract String getString();
}
