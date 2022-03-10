package io.github.smdawe

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.netflix.graphql.dgs.client.GraphQLResponse
import com.netflix.graphql.dgs.client.MonoGraphQLClient
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

import static com.github.tomakehurst.wiremock.client.WireMock.*

class WiremockGqlExamples extends Specification {

  @Shared
  WireMockServer mockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  @Shared
  String baseUrl

  String testUrl
  String testPath

  WebClientGraphQLClient client

  void setupSpec() {
    mockServer.start()
    baseUrl = "http://localhost:${mockServer.port()}"
  }

  void cleanupSpec() {
    mockServer.stop()
  }

  void setup() {
    testPath = '/graphql'
    testUrl = "$baseUrl$testPath"
    WebClient webClient = WebClient.create("http://localhost:8080/graphql");
    client = MonoGraphQLClient.createWithWebClient(webClient);

  }

  void 'graphql'() {
    given:
      String query = """
{
  example(id: "1") {
    field1
    field2
  }
}
      """
    when:
      Mono<GraphQLResponse> graphQLResponseMono = client.reactiveExecuteQuery(query);
    then:
      Mono<String> field = graphQLResponseMono.map(r -> r.extractValue("field1"));
      field.subscribe(result -> println(result))
      Thread.sleep(1000)
  }

}
