package org.infinispan.client.hotrod.impl.operations;

import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.Immutable;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.exceptions.InvalidResponseException;
import org.infinispan.client.hotrod.impl.protocol.Codec;
import org.infinispan.client.hotrod.impl.protocol.HeaderParams;
import org.infinispan.client.hotrod.impl.protocol.HotRodConstants;
import org.infinispan.client.hotrod.impl.transport.Transport;
import org.infinispan.commons.logging.BasicLogFactory;
import org.jboss.logging.BasicLogger;

/**
 * Corresponds to the "ping" operation as defined in <a href="http://community.jboss.org/wiki/HotRodProtocol">Hot Rod protocol specification</a>.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Immutable
public class PingOperation extends HotRodOperation {

   private static final BasicLogger log = BasicLogFactory.getLog(PingOperation.class);

   private final Transport transport;

   public PingOperation(Codec codec, AtomicInteger topologyId, Transport transport) {
      this(codec, topologyId, transport, DEFAULT_CACHE_NAME_BYTES);
   }

   public PingOperation(Codec codec, AtomicInteger topologyId, Transport transport, byte[] cacheName) {
      super(codec, null, cacheName, topologyId);
      this.transport = transport;
   }

   @Override
   public PingResult execute() {
      try {
         HeaderParams params = writeHeader(transport, HotRodConstants.PING_REQUEST);
         transport.flush();

         short respStatus = readHeaderAndValidate(transport, params);
         if (respStatus == HotRodConstants.NO_ERROR_STATUS) {
            if (log.isTraceEnabled())
               log.tracef("Successfully validated transport: %s", transport);
            return PingResult.SUCCESS;
         } else {
            String hexStatus = Integer.toHexString(respStatus);
            if (log.isTraceEnabled())
               log.tracef("Unknown response status: %s", hexStatus);

            throw new InvalidResponseException(
                  "Unexpected response status: " + hexStatus);
         }
      } catch (HotRodClientException e) {
         if (e.getMessage().contains("CacheNotFoundException"))
            return PingResult.CACHE_DOES_NOT_EXIST;

         // Any other situation, rethrow the exception
         throw e;
      }
   }

   public static enum PingResult {
      // Success if the ping request was responded correctly
      SUCCESS,
      // When the ping request fails due to non-existing cache
      CACHE_DOES_NOT_EXIST,
      // For any other type of failures
      FAIL,
   }
}
