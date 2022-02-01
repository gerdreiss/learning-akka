import java.security.{KeyStore, SecureRandom}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

package object ssl {

  lazy val httpsConnectionContext: HttpsConnectionContext =
    utils.using(getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")) { in =>

      val ksPwd = "akka-https".toCharArray
      val keyStore = KeyStore.getInstance("PKCS12")
      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
      val sslContext = SSLContext.getInstance("TLS")

      keyStore.load(in, ksPwd)
      keyManagerFactory.init(keyStore, ksPwd)
      trustManagerFactory.init(keyStore)
      sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

      ConnectionContext.https(sslContext)
    }
}
