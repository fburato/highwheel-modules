package org.pitest.highwheel.cycles;

import org.pitest.highwheel.model.ElementName;


public interface Filter {

  boolean include(ElementName item);

}
