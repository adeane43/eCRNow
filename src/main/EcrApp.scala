import com.drajer.bsa.utils.StartupUtils
import com.drajer.ecrapp.security.{AuthorizationService, RequestMDCFilter, SampleAuthorizationServiceImpl}

import java.time.Instant
import java.util.{Date, TimeZone}
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletComponentScan
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.context.event.EventListener
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.client.RestTemplate

import scala.compiletime.uninitialized

@main()
def main(args: Array[String]): Unit = {
  TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
  SpringApplication.run(classOf[EcrApp], args*)
}

@SpringBootApplication(exclude = Array(classOf[HibernateJpaAutoConfiguration]))
@EnableTransactionManagement
@ServletComponentScan
@Configuration
class EcrApp extends SpringBootServletInitializer {

  @Value("${rest.template.connection.timeout}")
  var connectionTimeOut: Int = uninitialized

  @Value("${rest.template.read.timeout}")
  var readTimeout: Int = uninitialized

  override def configure(application: SpringApplicationBuilder): SpringApplicationBuilder = {
    application.sources(classOf[EcrApp])
  }

  @EventListener(Array(classOf[ApplicationReadyEvent]))
  def onApplicationReady(): Unit = {
    StartupUtils.setStartTime(Date.from(Instant.now()))
  }

  @Bean
  def restTemplate(): RestTemplate = {
    val clientHttpRequestFactory = new SimpleClientHttpRequestFactory()
    clientHttpRequestFactory.setConnectTimeout(connectionTimeOut)
    clientHttpRequestFactory.setReadTimeout(readTimeout)
    new RestTemplate(clientHttpRequestFactory)
  }

  @Bean
  def authorizationService(): AuthorizationService = {
    new SampleAuthorizationServiceImpl()
  }

  @Bean
  def loggingFilter(): FilterRegistrationBean[RequestMDCFilter] = {
    val registrationBean = new FilterRegistrationBean[RequestMDCFilter]()
    registrationBean.setFilter(new RequestMDCFilter())
    registrationBean.addUrlPatterns("/api/*")
    registrationBean
  }
}
