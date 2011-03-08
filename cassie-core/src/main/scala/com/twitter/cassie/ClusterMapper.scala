package com.twitter.cassie

import scala.util.parsing.json.JSON
import java.io.IOException
import java.net.InetSocketAddress
import com.twitter.logging.Logger
import scala.collection.JavaConversions._
import com.twitter.cassie.connection.ClusterClientProvider

/**
 * Given a seed host and port, returns a set of nodes in the cluster.
 *
 * TODO: Accept a set of seedHosts
 *
 * @param keyspace the keyspace to map
 * @param seedHost the hostname of the seed node
 * @param seedPort the Thrift port of the seed node
 * @author coda
 */
private class ClusterMapper(keyspace: String, seedHost: String, seedPort: Int = 9160, timeoutMS: Int = 10000) {
  val log = Logger.get
  private val ccp = new ClusterClientProvider(
    Set(new InetSocketAddress(seedHost, seedPort)),
    keyspace,
    readTimeoutInMS = timeoutMS,
    maxConnectionsPerHost = 1
  )

  /**
   * @return The set of addresses of the nodes in the cluster, and close the mapper.
   */
  def perform(): Set[InetSocketAddress] = {
    log.info("Mapping cluster...")
    val ring = ccp.map{ _.describe_ring(keyspace) }()
    ccp.close
    log.debug("Received: %s", ring)
    val hosts = asScalaIterable(ring).flatMap{ h => asScalaIterable(h.endpoints).map{ host =>
      new InetSocketAddress(host, seedPort) } }.toSet
    hosts
  }
}
