package org.example.io;

import org.example.commons.Utility;
import org.example.core.api.IOInterface;
import org.example.core.model.Entity1;

public class IOImplementaion implements IOInterface {
    @Override
    public Entity1 reader() {
        Utility.util();
        Utility.util1();
        return new Entity1();
    }

    private void something() {
        Utility.util();
    }
}
