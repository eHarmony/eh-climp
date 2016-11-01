package com.eharmony.climp;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class OpenclResizerFactory extends BasePooledObjectFactory<OpenclResizer> {
    private final String shaderName;

    public OpenclResizerFactory(String shaderName) {
        this.shaderName = shaderName;
    }

    @Override
    public OpenclResizer create() throws Exception {
        return new OpenclResizer(shaderName);
    }

    @Override
    public PooledObject<OpenclResizer> wrap(OpenclResizer resizer) {
        return new DefaultPooledObject<OpenclResizer>(resizer);
    }

    @Override
    public void destroyObject(PooledObject<OpenclResizer> p) throws Exception {
        super.destroyObject(p);
        p.getObject().close();
    }

}
