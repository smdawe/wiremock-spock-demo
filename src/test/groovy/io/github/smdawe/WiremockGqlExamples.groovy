package io.github.smdawe

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.netflix.graphql.dgs.client.GraphQLResponse
import com.netflix.graphql.dgs.client.MonoGraphQLClient
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Shared
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.*

class WiremockGqlExamples extends Specification {

  @Shared
  WireMockServer mockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort().notifier(new Slf4jNotifier(true)))

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
    WebClient webClient = WebClient.create(testUrl);
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
      String response = """
{
  "data": {
    "field1":"test1",
    "field2":"test2"
  }
}
"""
      String actualQuery = '{"query":"\\n{\\n  example(id: \\"1\\") {\\n    field1\\n    field2\\n  }\\n}\\n","variables":{},"operationName":null}'

      mockServer.stubFor(post(testPath)
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalTo(actualQuery))
        .willReturn(okJson(response))
      );

    when:
      Mono<GraphQLResponse> graphQLResponseMono = client.reactiveExecuteQuery(query);
    then:
      Mono<String> field1 = graphQLResponseMono.map(r -> r.extractValue("field1"));
      field1.subscribe()
      field1.block() == "test1"
  }

}
