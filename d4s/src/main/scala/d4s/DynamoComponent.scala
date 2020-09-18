package d4s

import java.net.{URI, URL}

import d4s.config.DynamoConfig
import izumi.distage.framework.model.IntegrationCheck
import izumi.distage.model.definition.{DIResource, Id}
import izumi.functional.bio.{BIO, F}
import izumi.fundamentals.platform.integration.{PortCheck, ResourceCheck}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.apache.ApacheSdkHttpService
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

import scala.util.chaining._

final case class DynamoComponent(client: DynamoDbClient)

object DynamoComponent {

  final class Impl[F[+_, +_]: BIO](
    conf: DynamoConfig,
    portCheck: PortCheck @Id("dynamo-port"),
  ) extends DIResource[F[Throwable, ?], DynamoComponent]
    with IntegrationCheck {

    override def resourcesAvailable(): ResourceCheck = {
      conf.maybeLocalUrl.fold(ResourceCheck.Success(): ResourceCheck) {
        url =>
          portCheck.checkUrl(new URL(url), "DynamoClient")
      }
    }

    override def acquire: F[Throwable, DynamoComponent] = F.syncThrowable {
      DynamoComponent(mkSyncClient())
    }

    override def release(resource: DynamoComponent): F[Throwable, Unit] = F.syncThrowable {
      resource.client.close()
    }

    private[this] def mkSyncClient(): DynamoDbClient = {
      val builder = DynamoDbClient
        .builder()
        .httpClientBuilder(new ApacheSdkHttpService().createHttpClientBuilder())

      mkClient(builder)
    }

    private[this] def mkClient[B <: AwsClientBuilder[B, C], C](builder: AwsClientBuilder[B, C]): C = {
      val client = builder
        .pipe(setEndpoint)
        .overrideConfiguration(
          ClientOverrideConfiguration
            .builder()
            .apiCallAttemptTimeout(java.time.Duration.ofMillis(conf.connectionTimeout.toMillis))
            .build()
        )

      conf.getRegion match {
        case Some(value) => client.region(Region.of(value)).build()
        case None        => client.build()
      }
    }

    private[this] def setEndpoint[B <: AwsClientBuilder[B, C], C](builder: AwsClientBuilder[B, C]): AwsClientBuilder[B, C] = {
      conf.maybeLocalUrl.fold {
        builder
      } {
        url =>
          builder
            .endpointOverride(URI.create(url))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
      }
    }
  }

}
