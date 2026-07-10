package io.quarkiverse.snappy.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.xerial.snappy.pool.DefaultPoolFactory;

/**
 * Reports which {@code BufferPool} implementation {@link DefaultPoolFactory} selected inside the
 * deployed process. The selection is driven by the {@code org.xerial.snappy.pool.disable} system
 * property, read once in the static initializer of {@link DefaultPoolFactory}, so the value observed
 * here tells us when that initializer ran: on the build host or in the running application.
 */
@Path("/snappy/buffer-pool")
@ApplicationScoped
public class BufferPoolResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String activePool() {
        return DefaultPoolFactory.getDefaultPool().getClass().getSimpleName();
    }
}
